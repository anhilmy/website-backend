package com.anhilmy.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.anhilmy.pos.db.DbManager;
import com.anhilmy.pos.exception.BusinessRuleException;
import com.anhilmy.pos.model.MenuCategory;
import com.anhilmy.pos.model.MenuItem;
import com.anhilmy.pos.model.ModifierGroup;
import com.anhilmy.pos.model.ModifierOption;
import com.anhilmy.pos.model.PaymentMethod;
import com.anhilmy.pos.model.Roles;
import com.anhilmy.pos.model.SelectionType;
import com.anhilmy.pos.model.Transaction;
import com.anhilmy.pos.model.TransactionItem;
import com.anhilmy.pos.model.User;
import com.anhilmy.pos.repository.MenuCategoryRepository;
import com.anhilmy.pos.repository.MenuItemRepository;
import com.anhilmy.pos.repository.ModifierGroupRepository;
import com.anhilmy.pos.repository.ModifierOptionRepository;
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
    private final ModifierGroupRepository modifierGroupRepository = new ModifierGroupRepository();
    private final ModifierOptionRepository modifierOptionRepository = new ModifierOptionRepository();
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
    void moneyIsStoredAsExactMinorUnitsAndPriceFiltersUseTheSameScale() {
        String suffix = Long.toString(System.nanoTime());

        MenuCategory category = new MenuCategory();
        category.setName("Money_" + suffix);
        category.setActive(true);
        categoryRepository.save(category);

        MenuItem item = new MenuItem();
        item.setName("Exact_" + suffix);
        item.setPrice(new BigDecimal("12345.67"));
        item.setMenuCategory(category);
        item.setAvailable(true);
        menuItemRepository.save(item);

        Object stored = DbManager.getSession()
                .createNativeQuery("select price from menu_items where id = :id")
                .setParameter("id", item.getId())
                .getSingleResult();
        assertTrue(stored instanceof Number);
        assertEquals(1_234_567L, ((Number) stored).longValue());

        List<MenuItem> match = menuItemRepository.search("exact_" + suffix, null, null,
                new BigDecimal("12345.67"), new BigDecimal("12345.67"));
        assertEquals(1, match.size());

        List<MenuItem> tooCheap = menuItemRepository.search("exact_" + suffix, null, null,
                new BigDecimal("12345.68"), null);
        assertTrue(tooCheap.isEmpty());
    }

    @Test
    void modifierOptionsPriceLinesFreezeSnapshotsAndSurviveAmend() {
        String suffix = Long.toString(System.nanoTime());

        User cashier = new User();
        cashier.setUsername("modifier_" + suffix);
        cashier.setPasswordHash(PasswordUtil.hash("password1"));
        cashier.setRole(Roles.CASHIER);
        userRepository.save(cashier);

        MenuCategory category = new MenuCategory();
        category.setName("Beverages_" + suffix);
        category.setActive(true);
        categoryRepository.save(category);

        MenuItem item = new MenuItem();
        item.setName("Espresso_" + suffix);
        item.setPrice(new BigDecimal("18000.00"));
        item.setMenuCategory(category);
        item.setAvailable(true);
        menuItemRepository.save(item);

        ModifierGroup temperature = new ModifierGroup();
        temperature.setMenuItem(item);
        temperature.setName("Temperature");
        temperature.setSelectionType(SelectionType.SINGLE);
        temperature.setRequired(true);
        temperature.setMinSelect(1);
        temperature.setMaxSelect(1);
        temperature.setSortOrder(10);
        temperature.setActive(true);
        temperature.setCreatedAt(LocalDateTime.now());
        temperature.setUpdatedAt(LocalDateTime.now());
        modifierGroupRepository.save(temperature);

        ModifierOption hot = modifierOption(temperature, "Hot", "0", true);
        ModifierOption iced = modifierOption(temperature, "Iced", "0", false);
        modifierOptionRepository.save(hot);
        modifierOptionRepository.save(iced);

        Transaction order = service.createDraft(cashier.getId(), null);
        final Long orderId = order.getId();
        final Long itemId = item.getId();
        final Long hotId = hot.getId();
        final Long icedId = iced.getId();

        service.addItem(orderId, itemId, 1, List.of(hotId), "no sugar");
        service.addItem(orderId, itemId, 1, List.of(icedId), "no sugar");

        Transaction reloaded = service.getById(orderId);
        assertEquals(2, reloaded.getItems().size());

        // Same item + same option + same note merges.
        service.addItem(orderId, itemId, 1, List.of(hotId), "no sugar");
        reloaded = service.getById(orderId);
        assertEquals(2, reloaded.getItems().size());
        TransactionItem hotLine = lineWithOption(reloaded, hotId);
        assertEquals(2, hotLine.getQuantity());
        assertEquals(0, new BigDecimal("18000.00").compareTo(hotLine.getUnitPrice()));
        assertEquals(0, new BigDecimal("36000.00").compareTo(hotLine.getTotalPrice()));
        assertEquals("no sugar", hotLine.getNote());

        // Editing the menu option must not rewrite the historic snapshot.
        iced.setPriceDelta(new BigDecimal("5000.00"));
        modifierOptionRepository.update(iced);

        Transaction afterEdit = transactionRepository.findById(orderId).orElseThrow();
        TransactionItem icedLine = lineWithOption(afterEdit, icedId);
        assertEquals(0, new BigDecimal("18000.00").compareTo(icedLine.getUnitPrice()));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                icedLine.getModifiers().get(0).getPriceDelta()));

        // Amending a cancelled bill copies the note and option snapshots.
        service.cancel(orderId);
        Transaction amended = service.amend(orderId, cashier.getId());
        assertEquals(2, amended.getItems().size());
        TransactionItem amendedHot = lineWithOption(amended, hotId);
        assertEquals("no sugar", amendedHot.getNote());
        assertEquals("Temperature", amendedHot.getModifiers().get(0).getGroupName());
        assertEquals("Hot", amendedHot.getModifiers().get(0).getOptionName());
    }

    @Test
    void updateItemReplacesTheSnapshotOnARealDatabase() {
        String suffix = Long.toString(System.nanoTime());
        User cashier = newCashier("swap_" + suffix);
        MenuItem item = newMenuItem("Swap_" + suffix, "18000.00", "Beverages_" + suffix);
        ModifierGroup temperature = newGroup(item, "Temperature", SelectionType.SINGLE,
                true, 1, 1);
        ModifierOption hot = modifierOption(temperature, "Hot", "0", true);
        ModifierOption iced = modifierOption(temperature, "Iced", "0", false);
        modifierOptionRepository.save(hot);
        modifierOptionRepository.save(iced);

        Transaction order = service.createDraft(cashier.getId(), null);
        Long orderId = order.getId();
        service.addItem(orderId, item.getId(), 1, List.of(hot.getId()), "first");

        TransactionItem line = service.getById(orderId).getItems().get(0);
        Long lineId = line.getId();
        service.updateItem(orderId, lineId, null, List.of(iced.getId()), "swapped");

        // Re-open the session so orphan removal is proven against the database, not the cache.
        DbManager.commitAndClose();
        DbManager.openSession();

        Transaction reloaded = transactionRepository.findById(orderId).orElseThrow();
        TransactionItem updated = reloaded.getItems().get(0);
        assertEquals(1, updated.getModifiers().size());
        assertEquals("Iced", updated.getModifiers().get(0).getOptionName());
        assertEquals("swapped", updated.getNote());
        assertEquals(0, new BigDecimal("18000.00").compareTo(updated.getUnitPrice()));
    }

    @Test
    void refundOfAnOptionLineIncludesTheOptionPrice() {
        String suffix = Long.toString(System.nanoTime());
        User cashier = newCashier("refund_" + suffix);
        MenuItem item = newMenuItem("Refund_" + suffix, "18000.00", "Beverages_" + suffix);
        ModifierGroup extras = newGroup(item, "Extras", SelectionType.MULTI, false, 0, 3);
        ModifierOption shot = modifierOption(extras, "Extra shot", "5000", false);
        modifierOptionRepository.save(shot);

        Transaction order = service.createDraft(cashier.getId(), null);
        Long orderId = order.getId();
        order = service.addItem(orderId, item.getId(), 1, List.of(shot.getId()), null);
        assertEquals(0, new BigDecimal("23000.00").compareTo(
                order.getItems().get(0).getUnitPrice()));
        order = service.complete(orderId, "CASH", order.getTotalAmount());
        Long lineId = order.getItems().get(0).getId();
        BigDecimal total = order.getTotalAmount();

        Transaction refunded = service.refundItem(orderId, lineId, 1, "returned", null,
                "manager");

        assertEquals(1, refunded.getRefunds().size());
        assertEquals(0, total.compareTo(refunded.getRefunds().get(0).getAmount()));
        assertEquals(Transaction.Status.REFUNDED, refunded.getStatus());
    }

    private User newCashier(String username) {
        User cashier = new User();
        cashier.setUsername(username);
        cashier.setPasswordHash(PasswordUtil.hash("password1"));
        cashier.setRole(Roles.CASHIER);
        userRepository.save(cashier);
        return cashier;
    }

    private MenuItem newMenuItem(String name, String price, String categoryName) {
        MenuCategory category = new MenuCategory();
        category.setName(categoryName);
        category.setActive(true);
        categoryRepository.save(category);
        MenuItem item = new MenuItem();
        item.setName(name);
        item.setPrice(new BigDecimal(price));
        item.setMenuCategory(category);
        item.setAvailable(true);
        menuItemRepository.save(item);
        return item;
    }

    private ModifierGroup newGroup(MenuItem item, String name, SelectionType selectionType,
            boolean required, int minSelect, int maxSelect) {
        ModifierGroup group = new ModifierGroup();
        group.setMenuItem(item);
        group.setName(name);
        group.setSelectionType(selectionType);
        group.setRequired(required);
        group.setMinSelect(minSelect);
        group.setMaxSelect(maxSelect);
        group.setSortOrder(10);
        group.setActive(true);
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        modifierGroupRepository.save(group);
        return group;
    }

    private ModifierOption modifierOption(ModifierGroup group, String name, String priceDelta,
            boolean isDefault) {
        ModifierOption option = new ModifierOption();
        option.setGroup(group);
        option.setName(name);
        option.setPriceDelta(new BigDecimal(priceDelta));
        option.setIsDefault(isDefault);
        option.setAvailable(true);
        option.setSortOrder("Hot".equals(name) ? 10 : 20);
        option.setCreatedAt(LocalDateTime.now());
        option.setUpdatedAt(LocalDateTime.now());
        return option;
    }

    private static TransactionItem lineWithOption(Transaction tx, Long optionId) {
        return tx.getItems().stream()
                .filter(line -> line.getModifiers().stream()
                        .anyMatch(m -> optionId.equals(m.getModifierOptionId())))
                .findFirst()
                .orElseThrow();
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
