package com.anhilmy.pos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import com.anhilmy.pos.config.ConfigManager;
import com.anhilmy.pos.exception.BusinessRuleException;
import com.anhilmy.pos.exception.EntityNotFoundException;
import com.anhilmy.pos.model.MenuItem;
import com.anhilmy.pos.model.Transaction;
import com.anhilmy.pos.model.TransactionItem;
import com.anhilmy.pos.model.User;
import com.anhilmy.pos.repository.MenuItemRepository;
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

    public TransactionService() {
        this(new TransactionRepository(), new MenuItemRepository(), new UserRepository());
    }

    public TransactionService(TransactionRepository transactionRepository,
            MenuItemRepository menuItemRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.menuItemRepository = menuItemRepository;
        this.userRepository = userRepository;
    }

    public Transaction createDraft(Long cashierId, String customerInfo) {
        User cashier = userRepository.findById(cashierId)
                .orElseThrow(() -> new EntityNotFoundException("User", cashierId));
        Transaction tx = new Transaction();
        tx.setCashier(cashier);
        tx.setCustomerInfo(InputValidator.sanitize(customerInfo));
        tx.setStatus(Transaction.Status.DRAFT);
        tx.setTotalAmount(BigDecimal.ZERO);
        tx.setTaxAmount(BigDecimal.ZERO);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setUpdatedAt(LocalDateTime.now());
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

        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new EntityNotFoundException("MenuItem", menuItemId));
        if (!Boolean.TRUE.equals(menuItem.getAvailable())) {
            throw new BusinessRuleException(
                    "Menu item is not available: " + menuItem.getName());
        }

        // Merge with an existing line for the same item instead of adding a duplicate.
        TransactionItem existing = tx.getItems().stream()
                .filter(i -> i.getMenuItem().getId().equals(menuItemId))
                .findFirst().orElse(null);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + qty);
            recomputeLine(existing);
        } else {
            if (tx.getItems().size() >= MAX_ITEMS) {
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
        item.setQuantity(qty);
        recomputeLine(item);
        recalcTotals(tx);
        return transactionRepository.update(tx);
    }

    public Transaction removeItem(Long transactionId, Long transactionItemId) {
        Transaction tx = getById(transactionId);
        assertDraft(tx);
        TransactionItem item = findItem(tx, transactionItemId);
        tx.removeItem(item);
        recalcTotals(tx);
        return transactionRepository.update(tx);
    }

    public Transaction complete(Long transactionId, String paymentMethod,
            BigDecimal paymentAmount) {
        Transaction tx = getById(transactionId);
        assertDraft(tx);
        if (tx.getItems().isEmpty()) {
            throw new BusinessRuleException("Cannot complete an empty order");
        }
        String method = InputValidator.requireNonBlank(paymentMethod, "paymentMethod");
        BigDecimal paid = InputValidator.requirePositive(paymentAmount, "paymentAmount");
        recalcTotals(tx);
        if (paid.compareTo(tx.getTotalAmount()) < 0) {
            throw new BusinessRuleException(
                    "Payment amount is less than the order total " + tx.getTotalAmount());
        }
        tx.setPaymentMethod(method);
        tx.setPaymentAmount(scale(paid));
        tx.setStatus(Transaction.Status.COMPLETED);
        tx.setUpdatedAt(LocalDateTime.now());
        return transactionRepository.update(tx);
    }

    public Transaction cancel(Long transactionId) {
        Transaction tx = getById(transactionId);
        if (tx.getStatus() == Transaction.Status.COMPLETED
                || tx.getStatus() == Transaction.Status.CANCELLED) {
            throw new BusinessRuleException(
                    "Order in status " + tx.getStatus() + " cannot be cancelled");
        }
        tx.setStatus(Transaction.Status.CANCELLED);
        tx.setUpdatedAt(LocalDateTime.now());
        return transactionRepository.update(tx);
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

    private void recomputeLine(TransactionItem item) {
        item.setUnitPrice(scale(item.getUnitPrice()));
        item.setTotalPrice(scale(item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()))));
    }

    private void recalcTotals(Transaction tx) {
        BigDecimal subtotal = tx.getItems().stream()
                .map(TransactionItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = scale(subtotal.multiply(TAX_RATE));
        tx.setTaxAmount(tax);
        tx.setTotalAmount(scale(subtotal.add(tax)));
        tx.setUpdatedAt(LocalDateTime.now());
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal taxRate() {
        return TAX_RATE;
    }
}
