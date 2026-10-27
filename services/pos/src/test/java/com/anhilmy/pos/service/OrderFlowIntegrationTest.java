package com.anhilmy.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.anhilmy.pos.db.DbManager;
import com.anhilmy.pos.exception.BusinessRuleException;
import com.anhilmy.pos.model.MenuCategory;
import com.anhilmy.pos.model.MenuItem;
import com.anhilmy.pos.model.PaymentMethod;
import com.anhilmy.pos.model.Roles;
import com.anhilmy.pos.model.Transaction;
import com.anhilmy.pos.model.User;
import com.anhilmy.pos.repository.MenuCategoryRepository;
import com.anhilmy.pos.repository.MenuItemRepository;
import com.anhilmy.pos.repository.PaymentMethodRepository;
import com.anhilmy.pos.repository.TransactionRepository;
import com.anhilmy.pos.repository.UserRepository;
import com.anhilmy.pos.util.PasswordUtil;

/**
 * End-to-end order lifecycle against a real (SQLite) database, exercising Hibernate mappings,
 * the transaction boundary and the service business rules together.
 */
class OrderFlowIntegrationTest {

    private final UserRepository userRepository = new UserRepository();
    private final MenuCategoryRepository categoryRepository = new MenuCategoryRepository();
    private final MenuItemRepository menuItemRepository = new MenuItemRepository();
    private final TransactionRepository transactionRepository = new TransactionRepository();
    private final PaymentMethodRepository paymentMethodRepository = new PaymentMethodRepository();
    private final TransactionService service = new TransactionService();

    @BeforeEach
    void openSession() {
        DbManager.openSession();
        seedPaymentMethod("CASH", true);
        seedPaymentMethod("CARD", false);
    }

    private void seedPaymentMethod(String code, boolean isCash) {
        if (paymentMethodRepository.findByCode(code).isPresent()) {
            return;
        }
        PaymentMethod method = new PaymentMethod();
        method.setCode(code);
        method.setLabel(code);
        method.setActive(true);
        method.setIsCash(isCash);
        method.setRequiresReference(false);
        method.setSortOrder(0);
        method.setCreatedAt(LocalDateTime.now());
        method.setUpdatedAt(LocalDateTime.now());
        paymentMethodRepository.save(method);
    }

    @AfterEach
    void closeSession() {
        if (DbManager.hasSession()) {
            DbManager.commitAndClose();
        }
    }

    @Test
    void draftToCompletedOrderCalculatesTotalsAndBecomesImmutable() {
        String suffix = Long.toString(System.nanoTime());

        User cashier = new User();
        cashier.setUsername("cashier_" + suffix);
        cashier.setPasswordHash(PasswordUtil.hash("password1"));
        cashier.setRole(Roles.CASHIER);
        cashier.setCreatedAt(LocalDateTime.now());
        cashier.setUpdatedAt(LocalDateTime.now());
        userRepository.save(cashier);

        MenuCategory category = new MenuCategory();
        category.setName("Mains_" + suffix);
        category.setActive(true);
        categoryRepository.save(category);

        MenuItem item = new MenuItem();
        item.setName("Burger_" + suffix);
        item.setPrice(new BigDecimal("12.00"));
        item.setMenuCategory(category);
        item.setAvailable(true);
        menuItemRepository.save(item);

        Transaction order = service.createDraft(cashier.getId(), "Table 5");
        final Long orderId = order.getId();
        final Long itemId = item.getId();
        assertEquals(Transaction.Status.DRAFT, order.getStatus());

        order = service.addItem(orderId, itemId, 2);
        // subtotal 24.00, tax 2.40, total 26.40
        assertEquals(0, new BigDecimal("24.00").compareTo(
                order.getTotalAmount().subtract(order.getTaxAmount())));
        assertEquals(0, new BigDecimal("26.40").compareTo(order.getTotalAmount()));

        order = service.complete(orderId, "CASH", new BigDecimal("26.40"));
        assertEquals(Transaction.Status.COMPLETED, order.getStatus());

        // Persisted with the line item intact.
        Transaction reloaded = transactionRepository.findById(orderId).orElseThrow();
        assertEquals(1, reloaded.getItems().size());

        // Completed orders are immutable.
        assertThrows(BusinessRuleException.class, () -> service.addItem(orderId, itemId, 1));
    }

    @Test
    void cancelledDraftCannotBeModified() {
        String suffix = Long.toString(System.nanoTime());

        User cashier = new User();
        cashier.setUsername("cancel_" + suffix);
        cashier.setPasswordHash(PasswordUtil.hash("password1"));
        cashier.setRole(Roles.CASHIER);
        userRepository.save(cashier);

        Transaction order = service.createDraft(cashier.getId(), null);
        service.cancel(order.getId());

        assertEquals(Transaction.Status.CANCELLED, service.getById(order.getId()).getStatus());
        assertThrows(BusinessRuleException.class,
                () -> service.addItem(order.getId(), 1L, 1));
    }
}
