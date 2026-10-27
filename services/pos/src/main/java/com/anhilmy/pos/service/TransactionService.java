package com.anhilmy.pos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import com.anhilmy.pos.config.ConfigManager;
import com.anhilmy.pos.exception.BusinessRuleException;
import com.anhilmy.pos.exception.EntityNotFoundException;
import com.anhilmy.pos.model.Charge;
import com.anhilmy.pos.model.ChargeBasis;
import com.anhilmy.pos.model.ChargeCalcType;
import com.anhilmy.pos.model.ChargeTemplate;
import com.anhilmy.pos.model.MenuItem;
import com.anhilmy.pos.model.PaymentMethod;
import com.anhilmy.pos.model.Refund;
import com.anhilmy.pos.model.Transaction;
import com.anhilmy.pos.model.TransactionItem;
import com.anhilmy.pos.model.User;
import com.anhilmy.pos.repository.ChargeTemplateRepository;
import com.anhilmy.pos.repository.MenuItemRepository;
import com.anhilmy.pos.repository.PaymentMethodRepository;
import com.anhilmy.pos.repository.TransactionRepository;
import com.anhilmy.pos.repository.UserRepository;
import com.anhilmy.pos.util.InputValidator;

/**
 * Order lifecycle management.
 *
 * <p>Draft orders may be freely modified. Completing an order records payment and makes it
 * immutable; cancelled orders are terminal. Totals are always recomputed from line items so the
 * stored header figures can never drift from the items.
 */
public class TransactionService {

    private static final BigDecimal TAX_RATE = new BigDecimal(
            ConfigManager.get("order.tax-rate", "0.10"));
    private static final int MAX_ITEMS = ConfigManager.getInt("order.max-items", 200);

    private final TransactionRepository transactionRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final PaymentMethodService paymentMethodService;
    private final ChargeTemplateRepository chargeTemplateRepository;

    public TransactionService() {
        this(new TransactionRepository(), new MenuItemRepository(), new UserRepository(),
                new PaymentMethodRepository(), new ChargeTemplateRepository());
    }

    public TransactionService(TransactionRepository transactionRepository,
            MenuItemRepository menuItemRepository, UserRepository userRepository) {
        this(transactionRepository, menuItemRepository, userRepository,
                new PaymentMethodRepository(), new ChargeTemplateRepository());
    }

    public TransactionService(TransactionRepository transactionRepository,
            MenuItemRepository menuItemRepository, UserRepository userRepository,
            PaymentMethodRepository paymentMethodRepository) {
        this(transactionRepository, menuItemRepository, userRepository, paymentMethodRepository,
                new ChargeTemplateRepository());
    }

    public TransactionService(TransactionRepository transactionRepository,
            MenuItemRepository menuItemRepository, UserRepository userRepository,
            PaymentMethodRepository paymentMethodRepository,
            ChargeTemplateRepository chargeTemplateRepository) {
        this.transactionRepository = transactionRepository;
        this.menuItemRepository = menuItemRepository;
        this.userRepository = userRepository;
        this.paymentMethodService = new PaymentMethodService(paymentMethodRepository);
        this.chargeTemplateRepository = chargeTemplateRepository;
    }

    public Transaction createDraft(Long cashierId, String customerInfo) {
        User cashier = userRepository.findById(cashierId)
                .orElseThrow(() -> new EntityNotFoundException("User", cashierId));
        Transaction tx = new Transaction();
        tx.setCashier(cashier);
        tx.setCustomerInfo(InputValidator.sanitizeMax(customerInfo, "customerInfo", 255));
        tx.setStatus(Transaction.Status.DRAFT);
        tx.setSubtotalAmount(BigDecimal.ZERO);
        tx.setChargesAmount(BigDecimal.ZERO);
        tx.setTotalAmount(BigDecimal.ZERO);
        tx.setTaxAmount(BigDecimal.ZERO);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setUpdatedAt(LocalDateTime.now());
        applyChargeTemplates(tx);
        return transactionRepository.save(tx);
    }

    public Transaction getById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction", id));
    }

    public List<Transaction> list(Transaction.Status status, LocalDateTime from, LocalDateTime to) {
        return transactionRepository.findFiltered(status, from, to);
    }

    public Transaction addItem(Long transactionId, Long menuItemId, Integer quantity) {
        Transaction tx = getById(transactionId);
        assertDraft(tx);

        int qty = InputValidator.requirePositive(
                quantity == null ? 0 : quantity, "quantity");
        Long itemId = InputValidator.requireNonNull(menuItemId, "menuItemId");

        MenuItem menuItem = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("MenuItem", itemId));
        if (!Boolean.TRUE.equals(menuItem.getAvailable())) {
            throw new BusinessRuleException(
                    "Menu item is not available: " + menuItem.getName());
        }

        // Merge with an existing active line for the same item instead of adding a duplicate.
        TransactionItem existing = activeItems(tx).stream()
                .filter(i -> i.getMenuItem().getId().equals(itemId))
                .findFirst().orElse(null);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + qty);
            recomputeLine(existing);
        } else {
            if (activeItems(tx).size() >= MAX_ITEMS) {
                throw new BusinessRuleException("Order exceeds max items: " + MAX_ITEMS);
            }
            TransactionItem item = new TransactionItem();
            item.setMenuItem(menuItem);
            item.setQuantity(qty);
            item.setUnitPrice(scale(menuItem.getPrice()));
            recomputeLine(item);
            tx.addItem(item);
        }
        recalcTotals(tx);
        return transactionRepository.update(tx);
    }

    public Transaction updateItemQuantity(Long transactionId, Long transactionItemId,
            Integer quantity) {
        Transaction tx = getById(transactionId);
        assertDraft(tx);
        int qty = InputValidator.requirePositive(
                quantity == null ? 0 : quantity, "quantity");
        TransactionItem item = findItem(tx, transactionItemId);
        assertActive(item);
        item.setQuantity(qty);
        recomputeLine(item);
        recalcTotals(tx);
        return transactionRepository.update(tx);
    }

    /** Backwards-compatible delete: drafts void the line instead of losing history. */
    public Transaction removeItem(Long transactionId, Long transactionItemId) {
        return voidItem(transactionId, transactionItemId, "Removed at till", null);
    }

    /** Voids a draft line; the line stays on the bill, struck through, excluded from totals. */
    public Transaction voidItem(Long transactionId, Long transactionItemId, String reason,
            String voidedBy) {
        Transaction tx = getById(transactionId);
        assertDraft(tx);
        TransactionItem item = findItem(tx, transactionItemId);
        assertActive(item);

        item.setStatus(TransactionItem.Status.VOIDED);
        item.setVoidReason(InputValidator.requireMaxLength(reason, "reason", 255));
        item.setVoidedBy(voidedBy);
        item.setVoidedAt(LocalDateTime.now());
        recalcTotals(tx);
        return transactionRepository.update(tx);
    }

    public Transaction complete(Long transactionId, String paymentMethod,
            BigDecimal paymentAmount) {
        return complete(transactionId, paymentMethod, paymentAmount, null);
    }

    public Transaction complete(Long transactionId, String paymentMethod,
            BigDecimal paymentAmount, String paymentReference) {
        Transaction tx = getById(transactionId);
        assertDraft(tx);
        if (activeItems(tx).isEmpty()) {
            throw new BusinessRuleException("Cannot complete an empty order");
        }
        PaymentMethod method = paymentMethodService.requireActive(paymentMethod);
        String reference = InputValidator.sanitizeMax(paymentReference, "paymentReference", 255);
        if (Boolean.TRUE.equals(method.getRequiresReference())
                && (reference == null || reference.isBlank())) {
            throw new BusinessRuleException(
                    "Payment reference is required for " + method.getLabel());
        }
        BigDecimal paid = InputValidator.requirePositive(paymentAmount, "paymentAmount");
        recalcTotals(tx);
        if (paid.compareTo(tx.getTotalAmount()) < 0) {
            throw new BusinessRuleException(
                    "Payment amount is less than the order total " + tx.getTotalAmount());
        }
        tx.setPaymentMethod(method.getCode());
        tx.setPaymentAmount(scale(paid));
        tx.setPaymentReference(reference == null || reference.isBlank() ? null : reference);
        tx.setStatus(Transaction.Status.COMPLETED);
        tx.setUpdatedAt(LocalDateTime.now());
        return transactionRepository.update(tx);
    }

    public Transaction cancel(Long transactionId) {
        Transaction tx = getById(transactionId);
        if (tx.getStatus() != Transaction.Status.DRAFT
                && tx.getStatus() != Transaction.Status.IN_PROGRESS) {
            throw new BusinessRuleException(
                    "Order in status " + tx.getStatus() + " cannot be cancelled");
        }
        tx.setStatus(Transaction.Status.CANCELLED);
        tx.setUpdatedAt(LocalDateTime.now());
        return transactionRepository.update(tx);
    }

    /**
     * Refunds part or all of a settled line. The amount includes that line's proportional share
     * of order charges. The original totals are untouched; refunds are append-only.
     */
    public Transaction refundItem(Long transactionId, Long transactionItemId, Integer quantity,
            String reason, String paymentMethod, String createdBy) {
        Transaction tx = getById(transactionId);
        if (tx.getStatus() != Transaction.Status.COMPLETED
                && tx.getStatus() != Transaction.Status.PARTIALLY_REFUNDED) {
            throw new BusinessRuleException(
                    "Only settled bills can be refunded (current status: " + tx.getStatus() + ")");
        }
        Long itemId = InputValidator.requireNonNull(transactionItemId, "transactionItemId");
        TransactionItem item = findItem(tx, itemId);
        assertActive(item);

        int qty = InputValidator.requirePositive(quantity == null ? 0 : quantity, "quantity");
        int alreadyRefunded = tx.getRefunds().stream()
                .filter(r -> itemId.equals(r.getTransactionItemId()))
                .mapToInt(Refund::getQuantity)
                .sum();
        int remaining = item.getQuantity() - alreadyRefunded;
        if (qty > remaining) {
            throw new BusinessRuleException(
                    "Cannot refund " + qty + "; only " + remaining + " remain on this line");
        }

        BigDecimal subtotal = effectiveSubtotal(tx);
        BigDecimal lineTarget = subtotal.signum() > 0
                ? scale(item.getTotalPrice().multiply(tx.getTotalAmount())
                        .divide(subtotal, 2, RoundingMode.HALF_UP))
                : scale(item.getTotalPrice());
        BigDecimal alreadyAmount = scale(tx.getRefunds().stream()
                .filter(r -> itemId.equals(r.getTransactionItemId()))
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal amount;
        if (alreadyRefunded + qty == item.getQuantity()) {
            // Last unit of the line: clear the residual so rounding cannot leave a stuck partial.
            amount = scale(lineTarget.subtract(alreadyAmount));
        } else {
            BigDecimal lineAmount = scale(item.getTotalPrice()
                    .multiply(BigDecimal.valueOf(qty))
                    .divide(BigDecimal.valueOf(item.getQuantity()), 2, RoundingMode.HALF_UP));
            amount = subtotal.signum() > 0
                    ? scale(lineAmount.multiply(tx.getTotalAmount())
                            .divide(subtotal, 2, RoundingMode.HALF_UP))
                    : lineAmount;
        }
        BigDecimal outstanding = tx.getTotalAmount().subtract(tx.getRefundedAmount());
        if (amount.compareTo(outstanding) > 0) {
            amount = scale(outstanding);
        }
        if (amount.signum() <= 0) {
            throw new BusinessRuleException("Refund amount is zero");
        }

        String method = paymentMethod == null || paymentMethod.isBlank()
                ? tx.getPaymentMethod()
                : paymentMethodService.requireActive(paymentMethod).getCode();
        if (method == null || method.isBlank()) {
            throw new BusinessRuleException("A refund payment method is required");
        }

        Refund refund = new Refund();
        refund.setTransactionItemId(itemId);
        refund.setQuantity(qty);
        refund.setAmount(amount);
        refund.setReason(InputValidator.requireMaxLength(reason, "reason", 255));
        refund.setPaymentMethod(method);
        refund.setCreatedBy(createdBy);
        refund.setCreatedAt(LocalDateTime.now());
        tx.addRefund(refund);

        // Recompute from the records, never from a stale aggregate.
        BigDecimal refunded = scale(tx.getRefunds().stream()
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        tx.setRefundedAmount(refunded);
        tx.setStatus(refunded.compareTo(tx.getTotalAmount()) >= 0
                ? Transaction.Status.REFUNDED
                : Transaction.Status.PARTIALLY_REFUNDED);
        tx.setUpdatedAt(LocalDateTime.now());
        return transactionRepository.update(tx);
    }

    /**
     * Re-opens a cancelled bill as a new linked DRAFT. The old bill keeps its totals and is
     * marked AMENDED; the new bill is left unseated so the floor state stays explicit.
     */
    public Transaction amend(Long transactionId, Long cashierId) {
        Transaction source = getById(transactionId);
        if (source.getStatus() != Transaction.Status.CANCELLED) {
            throw new BusinessRuleException(
                    "Only CANCELLED bills can be amended (current status: " + source.getStatus()
                            + ")");
        }
        if (source.getAmendedToId() != null) {
            throw new BusinessRuleException("This bill has already been amended");
        }
        List<TransactionItem> sourceItems = activeItems(source);
        if (sourceItems.isEmpty()) {
            throw new BusinessRuleException("Cannot amend a bill with no active items");
        }

        Transaction clone = createDraft(cashierId, source.getCustomerInfo());
        for (TransactionItem line : sourceItems) {
            TransactionItem copy = new TransactionItem();
            copy.setMenuItem(line.getMenuItem());
            copy.setQuantity(line.getQuantity());
            copy.setUnitPrice(scale(line.getUnitPrice()));
            copy.setTotalPrice(scale(line.getTotalPrice()));
            clone.addItem(copy);
        }
        recalcTotals(clone);
        clone.setAmendedFromId(source.getId());

        source.setStatus(Transaction.Status.AMENDED);
        source.setAmendedToId(clone.getId());
        source.setUpdatedAt(LocalDateTime.now());
        transactionRepository.update(source);

        return transactionRepository.update(clone);
    }

    private TransactionItem findItem(Transaction tx, Long transactionItemId) {
        return tx.getItems().stream()
                .filter(i -> i.getId() != null && i.getId().equals(transactionItemId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("TransactionItem",
                        transactionItemId));
    }

    private void assertDraft(Transaction tx) {
        if (tx.getStatus() != Transaction.Status.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT orders can be modified (current status: " + tx.getStatus() + ")");
        }
    }

    private static void assertActive(TransactionItem item) {
        // Rows created before the status column existed have null; treat them as active.
        if (item.getStatus() != null && item.getStatus() != TransactionItem.Status.ACTIVE) {
            throw new BusinessRuleException("This line has been voided");
        }
    }

    private static List<TransactionItem> activeItems(Transaction tx) {
        return tx.getItems().stream()
                .filter(i -> i.getStatus() == null || i.getStatus() == TransactionItem.Status.ACTIVE)
                .toList();
    }

    private static BigDecimal effectiveSubtotal(Transaction tx) {
        BigDecimal stored = tx.getSubtotalAmount();
        if (stored != null && stored.signum() > 0) {
            return stored;
        }
        return scale(activeItems(tx).stream()
                .map(TransactionItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private void recomputeLine(TransactionItem item) {
        item.setUnitPrice(scale(item.getUnitPrice()));
        item.setTotalPrice(scale(item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()))));
    }

    private void recalcTotals(Transaction tx) {
        BigDecimal subtotal = activeItems(tx).stream()
                .map(TransactionItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        subtotal = scale(subtotal);
        tx.setSubtotalAmount(subtotal);

        if (tx.getCharges().isEmpty()) {
            // Bills created before charge templates existed keep the configured flat tax.
            BigDecimal tax = scale(subtotal.multiply(TAX_RATE));
            tx.setChargesAmount(tax);
            tx.setTaxAmount(tax);
            tx.setTotalAmount(scale(subtotal.add(tax)));
            tx.setUpdatedAt(LocalDateTime.now());
            return;
        }

        List<Charge> ordered = tx.getCharges().stream()
                .sorted(Comparator
                        .comparingInt((Charge c) -> c.getSortOrder() == null ? 0 : c.getSortOrder())
                        .thenComparingLong(c -> c.getId() == null ? 0L : c.getId()))
                .toList();

        BigDecimal runningBase = subtotal;
        BigDecimal chargesTotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;

        for (Charge charge : ordered) {
            BigDecimal base = charge.getBasis() == ChargeBasis.RUNNING_TOTAL
                    ? runningBase
                    : subtotal;
            BigDecimal amount = charge.getCalcType() == ChargeCalcType.FIXED
                    ? charge.getValue()
                    : base.multiply(charge.getValue());
            amount = scale(amount);
            charge.setComputedAmount(amount);
            charge.setUpdatedAt(LocalDateTime.now());

            chargesTotal = chargesTotal.add(amount);
            if (Boolean.TRUE.equals(charge.getIsTax())) {
                taxTotal = taxTotal.add(amount);
            }
            if (Boolean.TRUE.equals(charge.getTaxable())) {
                runningBase = runningBase.add(amount);
            }
        }

        tx.setChargesAmount(scale(chargesTotal));
        tx.setTaxAmount(scale(taxTotal));
        tx.setTotalAmount(scale(subtotal.add(chargesTotal)));
        tx.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Copies the active charge templates onto a new order. When none are configured, a default
     * tax charge is created from {@code order.tax-rate} so bills never lose their tax.
     */
    private void applyChargeTemplates(Transaction tx) {
        List<ChargeTemplate> templates = chargeTemplateRepository.findByActive(true);
        if (templates.isEmpty()) {
            tx.addCharge(charge("TAX", "Tax", ChargeCalcType.PERCENTAGE, TAX_RATE,
                    ChargeBasis.SUBTOTAL, 10, false, true));
            return;
        }
        for (ChargeTemplate template : templates) {
            tx.addCharge(charge(template.getCode(), template.getLabel(), template.getCalcType(),
                    template.getValue(), template.getBasis(), template.getSortOrder(),
                    Boolean.TRUE.equals(template.getTaxable()),
                    Boolean.TRUE.equals(template.getIsTax())));
        }
    }

    private static Charge charge(String code, String label, ChargeCalcType calcType,
            BigDecimal value, ChargeBasis basis, Integer sortOrder, boolean taxable,
            boolean isTax) {
        Charge charge = new Charge();
        charge.setCode(code);
        charge.setLabel(label);
        charge.setCalcType(calcType);
        charge.setValue(value);
        charge.setBasis(basis);
        charge.setSortOrder(sortOrder);
        charge.setTaxable(taxable);
        charge.setIsTax(isTax);
        charge.setComputedAmount(BigDecimal.ZERO);
        return charge;
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal taxRate() {
        return TAX_RATE;
    }
}
