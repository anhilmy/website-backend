package com.anhilmy.pos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.anhilmy.pos.config.ConfigManager;
import com.anhilmy.pos.dto.OrderChargeRequest;
import com.anhilmy.pos.exception.BusinessRuleException;
import com.anhilmy.pos.exception.EntityNotFoundException;
import com.anhilmy.pos.exception.ValidationException;
import com.anhilmy.pos.model.Charge;
import com.anhilmy.pos.model.ChargeBasis;
import com.anhilmy.pos.model.ChargeCalcType;
import com.anhilmy.pos.model.ChargeTemplate;
import com.anhilmy.pos.model.MenuItem;
import com.anhilmy.pos.model.ModifierGroup;
import com.anhilmy.pos.model.ModifierOption;
import com.anhilmy.pos.model.PaymentMethod;
import com.anhilmy.pos.model.Refund;
import com.anhilmy.pos.model.Transaction;
import com.anhilmy.pos.model.TransactionItem;
import com.anhilmy.pos.model.TransactionItemModifier;
import com.anhilmy.pos.model.User;
import com.anhilmy.pos.repository.ChargeTemplateRepository;
import com.anhilmy.pos.repository.MenuItemRepository;
import com.anhilmy.pos.repository.ModifierGroupRepository;
import com.anhilmy.pos.repository.ModifierOptionRepository;
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
    private final ModifierGroupRepository modifierGroupRepository;
    private final ModifierOptionRepository modifierOptionRepository;

    public TransactionService() {
        this(new TransactionRepository(), new MenuItemRepository(), new UserRepository(),
                new PaymentMethodRepository(), new ChargeTemplateRepository(),
                new ModifierGroupRepository(), new ModifierOptionRepository());
    }

    public TransactionService(TransactionRepository transactionRepository,
            MenuItemRepository menuItemRepository, UserRepository userRepository,
            PaymentMethodRepository paymentMethodRepository,
            ChargeTemplateRepository chargeTemplateRepository,
            ModifierGroupRepository modifierGroupRepository,
            ModifierOptionRepository modifierOptionRepository) {
        this.transactionRepository = transactionRepository;
        this.menuItemRepository = menuItemRepository;
        this.userRepository = userRepository;
        this.paymentMethodService = new PaymentMethodService(paymentMethodRepository);
        this.chargeTemplateRepository = chargeTemplateRepository;
        this.modifierGroupRepository = modifierGroupRepository;
        this.modifierOptionRepository = modifierOptionRepository;
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
        return addItem(transactionId, menuItemId, quantity, null, null);
    }

    /**
     * Adds a line, validating and snapshotting any selected options. Lines merge only when the
     * item, the option set and the note all match, so "Iced" and "Hot" stay separate.
     */
    public Transaction addItem(Long transactionId, Long menuItemId, Integer quantity,
            List<Long> modifierOptionIds, String note) {
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

        String cleanNote = normalizeNote(note);
        List<ModifierOption> selected = resolveSelections(menuItem, modifierOptionIds);
        List<Long> identity = selected.stream()
                .map(ModifierOption::getId)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        BigDecimal effectivePrice = effectiveUnitPrice(menuItem, selected);

        TransactionItem existing = activeItems(tx).stream()
                .filter(i -> sameLine(i, itemId, identity, cleanNote))
                .findFirst().orElse(null);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + qty);
            // The line is still a draft: re-quote it at the price the cashier just saw and
            // refresh the snapshot so the bill and the picker cannot disagree.
            existing.setUnitPrice(effectivePrice);
            applyModifiers(existing, selected);
            recomputeLine(existing);
        } else {
            if (activeItems(tx).size() >= MAX_ITEMS) {
                throw new BusinessRuleException("Order exceeds max items: " + MAX_ITEMS);
            }
            TransactionItem item = new TransactionItem();
            item.setMenuItem(menuItem);
            item.setQuantity(qty);
            item.setUnitPrice(effectivePrice);
            item.setNote(cleanNote);
            applyModifiers(item, selected);
            recomputeLine(item);
            tx.addItem(item);
        }
        recalcTotals(tx);
        return transactionRepository.update(tx);
    }

    /**
     * Updates a draft line. A null quantity/note leaves that field unchanged; a null option list
     * leaves the snapshot untouched, while an empty list clears it.
     */
    public Transaction updateItem(Long transactionId, Long transactionItemId, Integer quantity,
            List<Long> modifierOptionIds, String note) {
        Transaction tx = getById(transactionId);
        assertDraft(tx);
        TransactionItem item = findItem(tx, transactionItemId);
        assertActive(item);

        if (quantity != null) {
            item.setQuantity(InputValidator.requirePositive(quantity, "quantity"));
        }
        if (modifierOptionIds != null) {
            List<ModifierOption> selected = resolveSelections(item.getMenuItem(),
                    modifierOptionIds);
            item.setUnitPrice(effectiveUnitPrice(item.getMenuItem(), selected));
            applyModifiers(item, selected);
        }
        if (note != null) {
            item.setNote(normalizeNote(note));
        }
        if (modifierOptionIds != null || note != null) {
            assertLineIdentityUnique(tx, item);
        }
        recomputeLine(item);
        recalcTotals(tx);
        return transactionRepository.update(tx);
    }

    /**
     * Editing a line must not produce a second line with the same identity, or the terminal's
     * key-based cart sync would silently update only one of them.
     */
    private static void assertLineIdentityUnique(Transaction tx, TransactionItem item) {
        List<Long> identity = snapshotIdentity(item);
        if (identity == null) {
            return;
        }
        boolean duplicate = activeItems(tx).stream()
                .filter(other -> other != item)
                .anyMatch(other -> sameLine(other, item.getMenuItem().getId(), identity,
                        item.getNote()));
        if (duplicate) {
            throw new BusinessRuleException(
                    "Another line already has the same item, options and note");
        }
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
     * Adds an ad-hoc charge to a draft. A negative value is a discount; a negative percentage
     * is a percentage discount. The order total is never allowed to go below zero.
     */
    public Transaction addCharge(Long transactionId, OrderChargeRequest request) {
        Transaction tx = getById(transactionId);
        assertDraft(tx);

        Charge charge = new Charge();
        charge.setCode(normalizeChargeCode(request.code));
        charge.setLabel(InputValidator.requireMaxLength(request.label, "label", 64));
        charge.setCalcType(request.calcType == null
                ? ChargeCalcType.PERCENTAGE
                : request.calcType);
        charge.setValue(validateChargeValue(charge.getCalcType(), request.value));
        charge.setBasis(request.basis == null ? ChargeBasis.SUBTOTAL : request.basis);
        charge.setSortOrder(request.sortOrder == null
                ? nextChargeSortOrder(tx)
                : request.sortOrder);
        charge.setTaxable(request.taxable != null && request.taxable);
        charge.setIsTax(request.isTax != null && request.isTax);
        charge.setComputedAmount(BigDecimal.ZERO);
        charge.setCreatedAt(LocalDateTime.now());
        charge.setUpdatedAt(LocalDateTime.now());

        tx.addCharge(charge);
        recalcTotals(tx);
        return transactionRepository.update(tx);
    }

    /** Updates an ad-hoc charge while the bill is still a draft. Null fields stay unchanged. */
    public Transaction updateCharge(Long transactionId, Long chargeId,
            OrderChargeRequest request) {
        Transaction tx = getById(transactionId);
        assertDraft(tx);
        Charge charge = requireCharge(tx, chargeId);

        if (request.code != null) {
            charge.setCode(normalizeChargeCode(request.code));
        }
        if (request.label != null) {
            charge.setLabel(InputValidator.requireMaxLength(request.label, "label", 64));
        }
        if (request.calcType != null) {
            charge.setCalcType(request.calcType);
        }
        if (request.value != null) {
            charge.setValue(request.value);
        }
        charge.setValue(validateChargeValue(charge.getCalcType(), charge.getValue()));
        if (request.basis != null) {
            charge.setBasis(request.basis);
        }
        if (request.sortOrder != null) {
            charge.setSortOrder(request.sortOrder);
        }
        if (request.taxable != null) {
            charge.setTaxable(request.taxable);
        }
        if (request.isTax != null) {
            charge.setIsTax(request.isTax);
        }
        charge.setUpdatedAt(LocalDateTime.now());

        recalcTotals(tx);
        return transactionRepository.update(tx);
    }

    /** Removes an ad-hoc charge. Removing every charge leaves the bill at subtotal. */
    public Transaction removeCharge(Long transactionId, Long chargeId) {
        Transaction tx = getById(transactionId);
        assertDraft(tx);
        Charge charge = requireCharge(tx, chargeId);
        tx.removeCharge(charge);
        recalcTotals(tx);
        return transactionRepository.update(tx);
    }

    private static Charge requireCharge(Transaction tx, Long chargeId) {
        Long id = InputValidator.requireNonNull(chargeId, "chargeId");
        return tx.getCharges().stream()
                .filter(c -> c.getId() != null && c.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Charge", id));
    }

    private static String normalizeChargeCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return "AD_HOC";
        }
        String code = raw.trim().toUpperCase(java.util.Locale.ROOT);
        if (!code.matches("^[A-Z0-9_]{2,32}$")) {
            throw new ValidationException(
                    "code must be 2-32 characters of A-Z, 0-9 or underscore");
        }
        return code;
    }

    private static BigDecimal validateChargeValue(ChargeCalcType type, BigDecimal value) {
        BigDecimal clean = value == null ? BigDecimal.ZERO : value;
        if (type == ChargeCalcType.PERCENTAGE
                && clean.abs().compareTo(BigDecimal.ONE) > 0) {
            throw new ValidationException(
                    "percentage value must be between -1 and 1 (0.10 = 10%, -0.10 = discount)");
        }
        return clean;
    }

    private static int nextChargeSortOrder(Transaction tx) {
        return tx.getCharges().stream()
                .map(Charge::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .map(max -> max + 10)
                .orElse(10);
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
            copy.setNote(line.getNote());
            for (TransactionItemModifier original : line.getModifiers()) {
                TransactionItemModifier snapshot = new TransactionItemModifier();
                snapshot.setModifierOptionId(original.getModifierOptionId());
                snapshot.setGroupName(original.getGroupName());
                snapshot.setOptionName(original.getOptionName());
                snapshot.setPriceDelta(scale(original.getPriceDelta()));
                snapshot.setSortOrder(original.getSortOrder());
                snapshot.setCreatedAt(LocalDateTime.now());
                copy.addModifier(snapshot);
            }
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

    /**
     * Validates the requested option ids against the item's active groups and returns the
     * options in display order. Rules are authoritative here, not in the UI.
     */
    private List<ModifierOption> resolveSelections(MenuItem menuItem, List<Long> optionIds) {
        List<ModifierGroup> groups = modifierGroupRepository.findByMenuItemId(menuItem.getId(),
                true);
        List<Long> requested = optionIds == null ? List.of() : optionIds;

        Set<Long> unique = new LinkedHashSet<>();
        for (Long id : requested) {
            if (id == null) {
                throw new ValidationException("modifierOptionIds must not contain null");
            }
            if (!unique.add(id)) {
                throw new ValidationException("Duplicate modifier option: " + id);
            }
        }

        Map<Long, ModifierGroup> groupsById = new HashMap<>();
        for (ModifierGroup group : groups) {
            groupsById.put(group.getId(), group);
        }

        List<ModifierOption> selected = new ArrayList<>();
        Map<Long, Integer> counts = new HashMap<>();
        for (Long id : unique) {
            ModifierOption option = modifierOptionRepository.findById(id)
                    .orElseThrow(() -> new ValidationException(
                            "Unknown modifier option: " + id));
            if (option.getGroup() == null || option.getGroup().getId() == null
                    || !groupsById.containsKey(option.getGroup().getId())) {
                throw new BusinessRuleException("Option " + option.getName()
                        + " does not apply to " + menuItem.getName());
            }
            if (!Boolean.TRUE.equals(option.getAvailable())) {
                throw new BusinessRuleException("Option is not available: " + option.getName());
            }
            selected.add(option);
            counts.merge(option.getGroup().getId(), 1, Integer::sum);
        }

        for (ModifierGroup group : groups) {
            int count = counts.getOrDefault(group.getId(), 0);
            int min = group.getMinSelect() == null ? 0 : group.getMinSelect();
            int max = group.getMaxSelect() == null ? 1 : group.getMaxSelect();
            if (Boolean.TRUE.equals(group.getRequired()) && min < 1) {
                min = 1;
            }
            if (count < min) {
                throw new BusinessRuleException(
                        "Select at least " + min + " option(s) from " + group.getName());
            }
            if (count > max) {
                throw new BusinessRuleException(
                        "Select at most " + max + " option(s) from " + group.getName());
            }
        }

        selected.sort(Comparator
                .comparingInt((ModifierOption o) -> o.getGroup().getSortOrder() == null
                        ? 0
                        : o.getGroup().getSortOrder())
                .thenComparingInt(o -> o.getSortOrder() == null ? 0 : o.getSortOrder())
                .thenComparingLong(o -> o.getId() == null ? 0L : o.getId()));
        return selected;
    }

    private static BigDecimal effectiveUnitPrice(MenuItem menuItem,
            List<ModifierOption> options) {
        BigDecimal price = menuItem.getPrice();
        for (ModifierOption option : options) {
            if (option.getPriceDelta() != null) {
                price = price.add(option.getPriceDelta());
            }
        }
        BigDecimal effective = scale(price);
        if (effective.signum() < 0) {
            throw new BusinessRuleException("Options make the unit price negative for "
                    + menuItem.getName());
        }
        return effective;
    }

    private void applyModifiers(TransactionItem item, List<ModifierOption> selected) {
        item.clearModifiers();
        int ordinal = 0;
        for (ModifierOption option : selected) {
            TransactionItemModifier snapshot = new TransactionItemModifier();
            snapshot.setModifierOptionId(option.getId());
            snapshot.setGroupName(option.getGroup().getName());
            snapshot.setOptionName(option.getName());
            snapshot.setPriceDelta(scale(option.getPriceDelta() == null
                    ? BigDecimal.ZERO
                    : option.getPriceDelta()));
            // Stable display ordinal; the selection is already sorted by group/option order.
            snapshot.setSortOrder(ordinal);
            snapshot.setCreatedAt(LocalDateTime.now());
            item.addModifier(snapshot);
            ordinal++;
        }
    }

    /**
     * A line matches only when item, note and the sorted option-id set match. A line whose option
     * was deleted (null id) never merges, so a fresh selection becomes a new line.
     */
    private static boolean sameLine(TransactionItem line, Long menuItemId,
            List<Long> selectedIds, String note) {
        if (line.getMenuItem() == null || !menuItemId.equals(line.getMenuItem().getId())) {
            return false;
        }
        if (!Objects.equals(line.getNote(), note)) {
            return false;
        }
        List<Long> existingIds = snapshotIdentity(line);
        return existingIds != null && existingIds.equals(selectedIds);
    }

    /** Sorted option ids, or null when a snapshot lost its option id (deleted option). */
    private static List<Long> snapshotIdentity(TransactionItem line) {
        List<Long> ids = new ArrayList<>();
        for (TransactionItemModifier snapshot : line.getModifiers()) {
            if (snapshot.getModifierOptionId() == null) {
                return null;
            }
            ids.add(snapshot.getModifierOptionId());
        }
        Collections.sort(ids);
        return ids;
    }

    private static String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String clean = InputValidator.sanitizeMax(note, "note", 255);
        return clean == null || clean.isBlank() ? null : clean;
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
            // Removing every ad-hoc/template charge leaves the bill at subtotal; new orders
            // always receive at least the default tax charge from applyChargeTemplates().
            tx.setChargesAmount(BigDecimal.ZERO);
            tx.setTaxAmount(BigDecimal.ZERO);
            tx.setTotalAmount(subtotal);
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

        BigDecimal total = scale(subtotal.add(chargesTotal));
        if (total.signum() < 0) {
            throw new BusinessRuleException(
                    "Charges and discounts cannot reduce the order total below zero");
        }

        tx.setChargesAmount(scale(chargesTotal));
        tx.setTaxAmount(scale(taxTotal));
        tx.setTotalAmount(total);
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
