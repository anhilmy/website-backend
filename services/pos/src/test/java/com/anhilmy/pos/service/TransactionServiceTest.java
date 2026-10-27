package com.anhilmy.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anhilmy.pos.exception.BusinessRuleException;
import com.anhilmy.pos.model.Charge;
import com.anhilmy.pos.model.ChargeBasis;
import com.anhilmy.pos.model.ChargeCalcType;
import com.anhilmy.pos.model.ChargeTemplate;
import com.anhilmy.pos.model.MenuItem;
import com.anhilmy.pos.model.ModifierGroup;
import com.anhilmy.pos.model.ModifierOption;
import com.anhilmy.pos.model.PaymentMethod;
import com.anhilmy.pos.model.SelectionType;
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

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private ChargeTemplateRepository chargeTemplateRepository;
    @Mock
    private ModifierGroupRepository modifierGroupRepository;
    @Mock
    private ModifierOptionRepository modifierOptionRepository;

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(transactionRepository, menuItemRepository,
                userRepository, paymentMethodRepository, chargeTemplateRepository,
                modifierGroupRepository, modifierOptionRepository);
        lenient().when(paymentMethodRepository.findByCode("CASH"))
                .thenReturn(Optional.of(paymentMethod("CASH", true, false)));
        lenient().when(paymentMethodRepository.findByCode("CARD"))
                .thenReturn(Optional.of(paymentMethod("CARD", true, false)));
    }

    private Charge charge(String code, ChargeCalcType type, String value, ChargeBasis basis,
            int sortOrder, boolean taxable, boolean isTax) {
        Charge charge = new Charge();
        charge.setCode(code);
        charge.setLabel(code);
        charge.setCalcType(type);
        charge.setValue(new BigDecimal(value));
        charge.setBasis(basis);
        charge.setSortOrder(sortOrder);
        charge.setTaxable(taxable);
        charge.setIsTax(isTax);
        charge.setComputedAmount(BigDecimal.ZERO);
        return charge;
    }

    private TransactionItem line(String unitPrice, int quantity) {
        TransactionItem line = new TransactionItem();
        line.setMenuItem(menuItem(10L, unitPrice, true));
        line.setQuantity(quantity);
        line.setUnitPrice(new BigDecimal(unitPrice));
        line.setTotalPrice(new BigDecimal(unitPrice).multiply(BigDecimal.valueOf(quantity)));
        return line;
    }

    private ModifierGroup group(long id, String name, SelectionType type, boolean required,
            int min, int max) {
        ModifierGroup group = new ModifierGroup();
        group.setId(id);
        group.setName(name);
        group.setSelectionType(type);
        group.setRequired(required);
        group.setMinSelect(min);
        group.setMaxSelect(max);
        group.setSortOrder(0);
        group.setActive(true);
        MenuItem item = new MenuItem();
        item.setId(10L);
        group.setMenuItem(item);
        return group;
    }

    private ModifierOption option(long id, ModifierGroup group, String name, String priceDelta,
            boolean available) {
        ModifierOption option = new ModifierOption();
        option.setId(id);
        option.setGroup(group);
        option.setName(name);
        option.setPriceDelta(new BigDecimal(priceDelta));
        option.setIsDefault(false);
        option.setAvailable(available);
        option.setSortOrder(0);
        return option;
    }

    private PaymentMethod paymentMethod(String code, boolean active, boolean requiresReference) {
        PaymentMethod method = new PaymentMethod();
        method.setCode(code);
        method.setLabel(code);
        method.setActive(active);
        method.setIsCash("CASH".equals(code));
        method.setRequiresReference(requiresReference);
        return method;
    }

    private MenuItem menuItem(long id, String price, boolean available) {
        MenuItem item = new MenuItem();
        item.setId(id);
        item.setName("Item " + id);
        item.setPrice(new BigDecimal(price));
        item.setAvailable(available);
        return item;
    }

    private Transaction draft(long id) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setStatus(Transaction.Status.DRAFT);
        User cashier = new User();
        cashier.setId(1L);
        tx.setCashier(cashier);
        tx.setCreatedAt(LocalDateTime.now());
        return tx;
    }

    @Test
    void addItemComputesLineAndOrderTotalsWithTax() {
        Transaction tx = draft(1L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem(10L, "10.00", true)));
        lenient().when(transactionRepository.update(tx)).thenReturn(tx);

        service.addItem(1L, 10L, 2);

        assertEquals(1, tx.getItems().size());
        TransactionItem line = tx.getItems().get(0);
        assertEquals(0, new BigDecimal("20.00").compareTo(line.getTotalPrice()));
        // subtotal 20.00, tax 10% = 2.00, total 22.00
        assertEquals(0, new BigDecimal("2.00").compareTo(tx.getTaxAmount()));
        assertEquals(0, new BigDecimal("22.00").compareTo(tx.getTotalAmount()));
    }

    @Test
    void addItemMergesDuplicateLines() {
        Transaction tx = draft(1L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem(10L, "10.00", true)));
        lenient().when(transactionRepository.update(tx)).thenReturn(tx);

        service.addItem(1L, 10L, 1);
        service.addItem(1L, 10L, 2);

        assertEquals(1, tx.getItems().size());
        assertEquals(3, tx.getItems().get(0).getQuantity());
    }

    @Test
    void addItemRejectsUnavailableMenuItem() {
        Transaction tx = draft(1L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(menuItemRepository.findById(10L))
                .thenReturn(Optional.of(menuItem(10L, "10.00", false)));

        assertThrows(BusinessRuleException.class, () -> service.addItem(1L, 10L, 1));
    }

    @Test
    void completeRequiresPaymentToCoverTotal() {
        Transaction tx = draft(1L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem(10L, "10.00", true)));
        lenient().when(transactionRepository.update(tx)).thenReturn(tx);
        service.addItem(1L, 10L, 1);

        assertThrows(BusinessRuleException.class,
                () -> service.complete(1L, "CASH", new BigDecimal("5.00")));
    }

    @Test
    void completeSetsStatusAndPayment() {
        Transaction tx = draft(1L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem(10L, "10.00", true)));
        lenient().when(transactionRepository.update(tx)).thenReturn(tx);
        service.addItem(1L, 10L, 1);

        service.complete(1L, "CARD", new BigDecimal("11.00"));

        assertEquals(Transaction.Status.COMPLETED, tx.getStatus());
        assertEquals("CARD", tx.getPaymentMethod());
        assertEquals(0, new BigDecimal("11.00").compareTo(tx.getPaymentAmount()));
    }

    @Test
    void completePersistsPaymentReference() {
        Transaction tx = draft(1L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem(10L, "10.00", true)));
        lenient().when(transactionRepository.update(tx)).thenReturn(tx);
        service.addItem(1L, 10L, 1);

        service.complete(1L, "CARD", new BigDecimal("11.00"), "  txn-123  ");

        assertEquals("txn-123", tx.getPaymentReference());
    }

    @Test
    void completeRejectsUnknownPaymentMethod() {
        Transaction tx = draft(1L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem(10L, "10.00", true)));
        lenient().when(transactionRepository.update(tx)).thenReturn(tx);
        service.addItem(1L, 10L, 1);
        when(paymentMethodRepository.findByCode("BITCOIN")).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> service.complete(1L, "bitcoin", new BigDecimal("11.00")));
    }

    @Test
    void completeRequiresReferenceWhenMethodDeclaresIt() {
        Transaction tx = draft(1L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem(10L, "10.00", true)));
        lenient().when(transactionRepository.update(tx)).thenReturn(tx);
        service.addItem(1L, 10L, 1);
        when(paymentMethodRepository.findByCode("CARD"))
                .thenReturn(Optional.of(paymentMethod("CARD", true, true)));

        assertThrows(BusinessRuleException.class,
                () -> service.complete(1L, "CARD", new BigDecimal("11.00")));
    }

    @Test
    void chargesEvaluateInOrderWithRunningTaxableBase() {
        Transaction tx = draft(1L);
        tx.addItem(line("100.00", 1));
        tx.addCharge(charge("SERVICE", ChargeCalcType.PERCENTAGE, "0.10",
                ChargeBasis.SUBTOTAL, 10, true, false));
        tx.addCharge(charge("TAX", ChargeCalcType.PERCENTAGE, "0.10",
                ChargeBasis.RUNNING_TOTAL, 20, false, true));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        lenient().when(transactionRepository.update(tx)).thenReturn(tx);

        service.complete(1L, "CASH", new BigDecimal("200.00"));

        // service 10% of 100 = 10; tax 10% of (100 + 10) = 11; total 121.
        assertEquals(0, new BigDecimal("100.00").compareTo(tx.getSubtotalAmount()));
        assertEquals(0, new BigDecimal("21.00").compareTo(tx.getChargesAmount()));
        assertEquals(0, new BigDecimal("11.00").compareTo(tx.getTaxAmount()));
        assertEquals(0, new BigDecimal("121.00").compareTo(tx.getTotalAmount()));
    }

    @Test
    void fixedChargeAddsAbsoluteAmount() {
        Transaction tx = draft(1L);
        tx.addItem(line("100.00", 1));
        tx.addCharge(charge("DELIVERY", ChargeCalcType.FIXED, "5.00",
                ChargeBasis.SUBTOTAL, 10, false, false));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        lenient().when(transactionRepository.update(tx)).thenReturn(tx);

        service.complete(1L, "CASH", new BigDecimal("200.00"));

        assertEquals(0, new BigDecimal("5.00").compareTo(tx.getChargesAmount()));
        assertEquals(0, new BigDecimal("105.00").compareTo(tx.getTotalAmount()));
    }

    @Test
    void createDraftCopiesActiveTemplates() {
        User cashier = new User();
        cashier.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(cashier));

        ChargeTemplate template = new ChargeTemplate();
        template.setCode("SERVICE");
        template.setLabel("Service");
        template.setCalcType(ChargeCalcType.PERCENTAGE);
        template.setValue(new BigDecimal("0.05"));
        template.setBasis(ChargeBasis.SUBTOTAL);
        template.setSortOrder(5);
        template.setTaxable(false);
        template.setIsTax(false);
        when(chargeTemplateRepository.findByActive(true)).thenReturn(List.of(template));
        when(transactionRepository.save(org.mockito.ArgumentMatchers.any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Transaction tx = service.createDraft(1L, null);

        assertEquals(1, tx.getCharges().size());
        assertEquals("SERVICE", tx.getCharges().get(0).getCode());
        assertEquals(0, new BigDecimal("0.05").compareTo(tx.getCharges().get(0).getValue()));
    }

    @Test
    void createDraftFallsBackToDefaultTaxWhenNoTemplates() {
        User cashier = new User();
        cashier.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(cashier));
        when(chargeTemplateRepository.findByActive(true)).thenReturn(List.of());
        when(transactionRepository.save(org.mockito.ArgumentMatchers.any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Transaction tx = service.createDraft(1L, null);

        assertEquals(1, tx.getCharges().size());
        assertEquals("TAX", tx.getCharges().get(0).getCode());
        assertEquals(Boolean.TRUE, tx.getCharges().get(0).getIsTax());
    }

    @Test
    void completedOrderCannotBeModified() {
        Transaction tx = draft(1L);
        tx.setStatus(Transaction.Status.COMPLETED);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));

        assertThrows(BusinessRuleException.class, () -> service.addItem(1L, 10L, 1));
    }

    @Test
    void cancelRejectsCompletedOrder() {
        Transaction tx = draft(1L);
        tx.setStatus(Transaction.Status.COMPLETED);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));

        assertThrows(BusinessRuleException.class, () -> service.cancel(1L));
    }

    @Test
    void cannotCompleteEmptyOrder() {
        Transaction tx = draft(1L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));

        assertThrows(BusinessRuleException.class,
                () -> service.complete(1L, "CASH", new BigDecimal("10.00")));
    }

    @Test
    void voidItemExcludesLineFromTotals() {
        Transaction tx = draft(1L);
        TransactionItem line = line("100.00", 1);
        line.setId(10L);
        tx.addItem(line);
        tx.addCharge(charge("TAX", ChargeCalcType.PERCENTAGE, "0.10",
                ChargeBasis.SUBTOTAL, 10, false, true));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        lenient().when(transactionRepository.update(tx)).thenReturn(tx);

        service.voidItem(1L, 10L, "Wrong item", "cashier");

        assertEquals(TransactionItem.Status.VOIDED, line.getStatus());
        assertEquals("Wrong item", line.getVoidReason());
        assertEquals("cashier", line.getVoidedBy());
        assertEquals(0, BigDecimal.ZERO.compareTo(tx.getSubtotalAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(tx.getTotalAmount()));
    }

    @Test
    void voidedLineCannotBeVoidedTwice() {
        Transaction tx = draft(1L);
        TransactionItem line = line("100.00", 1);
        line.setId(10L);
        tx.addItem(line);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        lenient().when(transactionRepository.update(tx)).thenReturn(tx);
        service.voidItem(1L, 10L, "Wrong item", "cashier");

        assertThrows(BusinessRuleException.class,
                () -> service.voidItem(1L, 10L, "Again", "cashier"));
    }

    @Test
    void refundIsProportionalToOrderCharges() {
        Transaction tx = settledBill();
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        lenient().when(transactionRepository.update(tx)).thenReturn(tx);

        service.refundItem(1L, 10L, 1, "Customer changed mind", null, "manager");

        // 1 of 2 x 100 = 100 item; 220 total / 200 subtotal => 110 refund.
        assertEquals(1, tx.getRefunds().size());
        assertEquals(0, new BigDecimal("110.00").compareTo(tx.getRefundedAmount()));
        assertEquals(Transaction.Status.PARTIALLY_REFUNDED, tx.getStatus());

        service.refundItem(1L, 10L, 1, "Rest of the line", null, "manager");

        assertEquals(2, tx.getRefunds().size());
        assertEquals(0, new BigDecimal("220.00").compareTo(tx.getRefundedAmount()));
        assertEquals(Transaction.Status.REFUNDED, tx.getStatus());
    }

    @Test
    void refundRejectsQuantityBeyondRemaining() {
        Transaction tx = settledBill();
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));

        assertThrows(BusinessRuleException.class,
                () -> service.refundItem(1L, 10L, 3, "Too many", null, "manager"));
    }

    @Test
    void refundRequiresSettledBill() {
        Transaction tx = draft(1L);
        TransactionItem line = line("100.00", 1);
        line.setId(10L);
        tx.addItem(line);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));

        assertThrows(BusinessRuleException.class,
                () -> service.refundItem(1L, 10L, 1, "Nope", null, "manager"));
    }

    @Test
    void amendClonesActiveItemsAndLinksBothSides() {
        Transaction source = draft(5L);
        source.setStatus(Transaction.Status.CANCELLED);
        TransactionItem active = line("100.00", 1);
        TransactionItem voided = line("50.00", 1);
        voided.setStatus(TransactionItem.Status.VOIDED);
        source.addItem(active);
        source.addItem(voided);

        User cashier = new User();
        cashier.setId(1L);
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(source));
        when(userRepository.findById(1L)).thenReturn(Optional.of(cashier));
        when(chargeTemplateRepository.findByActive(true)).thenReturn(List.of());
        when(transactionRepository.save(org.mockito.ArgumentMatchers.any(Transaction.class)))
                .thenAnswer(inv -> {
                    Transaction saved = inv.getArgument(0);
                    saved.setId(99L);
                    return saved;
                });
        lenient().when(transactionRepository.update(org.mockito.ArgumentMatchers.any(
                Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction clone = service.amend(5L, 1L);

        assertEquals(1, clone.getItems().size());
        assertEquals(10L, clone.getItems().get(0).getMenuItem().getId());
        assertEquals(5L, clone.getAmendedFromId());
        assertEquals(99L, source.getAmendedToId());
        assertEquals(Transaction.Status.AMENDED, source.getStatus());
    }

    @Test
    void amendRejectsNonCancelledBill() {
        Transaction tx = draft(1L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));

        assertThrows(BusinessRuleException.class, () -> service.amend(1L, 1L));
    }

    @Test
    void chargesEvaluateBySortOrderNotListOrder() {
        Transaction tx = draft(1L);
        tx.addItem(line("100.00", 1));
        tx.addCharge(charge("TAX", ChargeCalcType.PERCENTAGE, "0.10",
                ChargeBasis.RUNNING_TOTAL, 20, false, true));
        tx.addCharge(charge("SERVICE", ChargeCalcType.PERCENTAGE, "0.10",
                ChargeBasis.SUBTOTAL, 10, true, false));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        lenient().when(transactionRepository.update(tx)).thenReturn(tx);

        service.complete(1L, "CASH", new BigDecimal("200.00"));

        // Service (sort 10) first: 10; tax (sort 20) on running 110: 11.
        assertEquals(0, new BigDecimal("21.00").compareTo(tx.getChargesAmount()));
        assertEquals(0, new BigDecimal("11.00").compareTo(tx.getTaxAmount()));
    }

    @Test
    void finalRefundClearsRoundingResidual() {
        Transaction tx = draft(1L);
        tx.setStatus(Transaction.Status.COMPLETED);
        tx.setPaymentMethod("CASH");
        TransactionItem line = line("33.33", 3);
        line.setId(10L);
        line.setTotalPrice(new BigDecimal("100.00"));
        tx.addItem(line);
        tx.setSubtotalAmount(new BigDecimal("100.00"));
        tx.setTotalAmount(new BigDecimal("100.00"));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        lenient().when(transactionRepository.update(tx)).thenReturn(tx);

        service.refundItem(1L, 10L, 1, "one", null, "manager");
        service.refundItem(1L, 10L, 1, "two", null, "manager");
        service.refundItem(1L, 10L, 1, "three", null, "manager");

        assertEquals(0, new BigDecimal("100.00").compareTo(tx.getRefundedAmount()));
        assertEquals(Transaction.Status.REFUNDED, tx.getStatus());
        assertEquals(0, new BigDecimal("33.34").compareTo(
                tx.getRefunds().get(2).getAmount()));
    }

    @Test
    void addItemPricesSelectedOptionsPerUnitAndSnapshotsThem() {
        Transaction tx = draft(1L);
        ModifierGroup group = group(5L, "Extras", SelectionType.MULTI, false, 0, 3);
        ModifierOption extra = option(10L, group, "Extra shot", "5000", true);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem(10L, "18000.00", true)));
        when(modifierGroupRepository.findByMenuItemId(10L, true)).thenReturn(List.of(group));
        when(modifierOptionRepository.findById(10L)).thenReturn(Optional.of(extra));
        lenient().when(transactionRepository.update(tx)).thenReturn(tx);

        service.addItem(1L, 10L, 2, List.of(10L), null);

        TransactionItem line = tx.getItems().get(0);
        assertEquals(0, new BigDecimal("23000.00").compareTo(line.getUnitPrice()));
        assertEquals(0, new BigDecimal("46000.00").compareTo(line.getTotalPrice()));
        assertEquals(1, line.getModifiers().size());
        assertEquals("Extra shot", line.getModifiers().get(0).getOptionName());
        assertEquals("Extras", line.getModifiers().get(0).getGroupName());
        assertEquals(0, new BigDecimal("5000.00").compareTo(
                line.getModifiers().get(0).getPriceDelta()));
    }

    @Test
    void addItemMergesOnlyMatchingOptionsAndNote() {
        Transaction tx = draft(1L);
        ModifierGroup group = group(5L, "Extras", SelectionType.MULTI, false, 0, 3);
        ModifierOption extraShot = option(10L, group, "Extra shot", "5000", true);
        ModifierOption oatMilk = option(11L, group, "Oat milk", "8000", true);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem(10L, "18000.00", true)));
        when(modifierGroupRepository.findByMenuItemId(10L, true)).thenReturn(List.of(group));
        when(modifierOptionRepository.findById(10L)).thenReturn(Optional.of(extraShot));
        when(modifierOptionRepository.findById(11L)).thenReturn(Optional.of(oatMilk));
        lenient().when(transactionRepository.update(tx)).thenReturn(tx);

        service.addItem(1L, 10L, 1, List.of(10L), "no sugar");
        service.addItem(1L, 10L, 1, List.of(10L), "no sugar");
        assertEquals(1, tx.getItems().size());
        assertEquals(2, tx.getItems().get(0).getQuantity());

        service.addItem(1L, 10L, 1, List.of(11L), "no sugar");
        assertEquals(2, tx.getItems().size());

        service.addItem(1L, 10L, 1, List.of(10L), "extra hot");
        assertEquals(3, tx.getItems().size());
    }

    @Test
    void addItemRequiresRequiredGroupSelection() {
        Transaction tx = draft(1L);
        ModifierGroup temperature = group(5L, "Temperature", SelectionType.SINGLE, true, 1, 1);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem(10L, "18000.00", true)));
        when(modifierGroupRepository.findByMenuItemId(10L, true))
                .thenReturn(List.of(temperature));

        assertThrows(BusinessRuleException.class,
                () -> service.addItem(1L, 10L, 1, null, null));
    }

    @Test
    void addItemRejectsDuplicateAndUnavailableOptions() {
        Transaction tx = draft(1L);
        ModifierGroup group = group(5L, "Extras", SelectionType.MULTI, false, 0, 3);
        ModifierOption soldOut = option(10L, group, "Sold out", "1000", false);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem(10L, "18000.00", true)));
        when(modifierGroupRepository.findByMenuItemId(10L, true)).thenReturn(List.of(group));
        when(modifierOptionRepository.findById(10L)).thenReturn(Optional.of(soldOut));

        assertThrows(com.anhilmy.pos.exception.ValidationException.class,
                () -> service.addItem(1L, 10L, 1, List.of(10L, 10L), null));
        assertThrows(BusinessRuleException.class,
                () -> service.addItem(1L, 10L, 1, List.of(10L), null));
    }

    @Test
    void addItemRejectsOptionFromAnotherItem() {
        Transaction tx = draft(1L);
        ModifierGroup ownGroup = group(5L, "Extras", SelectionType.MULTI, false, 0, 3);
        ModifierGroup otherGroup = group(6L, "Other", SelectionType.MULTI, false, 0, 3);
        ModifierOption otherOption = option(20L, otherGroup, "Other option", "0", true);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem(10L, "18000.00", true)));
        when(modifierGroupRepository.findByMenuItemId(10L, true)).thenReturn(List.of(ownGroup));
        when(modifierOptionRepository.findById(20L)).thenReturn(Optional.of(otherOption));

        assertThrows(BusinessRuleException.class,
                () -> service.addItem(1L, 10L, 1, List.of(20L), null));
    }

    @Test
    void addItemRejectsNegativeEffectivePrice() {
        Transaction tx = draft(1L);
        ModifierGroup group = group(5L, "Discount", SelectionType.MULTI, false, 0, 1);
        ModifierOption heavyDiscount = option(10L, group, "Heavy discount", "-5000", true);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem(10L, "1000.00", true)));
        when(modifierGroupRepository.findByMenuItemId(10L, true)).thenReturn(List.of(group));
        when(modifierOptionRepository.findById(10L)).thenReturn(Optional.of(heavyDiscount));

        assertThrows(BusinessRuleException.class,
                () -> service.addItem(1L, 10L, 1, List.of(10L), null));
    }

    @Test
    void updateItemReplacesOptionsAndRecomputesUnitPrice() {
        Transaction tx = draft(1L);
        TransactionItem line = line("18000.00", 1);
        line.setId(20L);
        tx.addItem(line);
        ModifierGroup group = group(5L, "Extras", SelectionType.MULTI, false, 0, 3);
        ModifierOption extra = option(10L, group, "Extra shot", "5000", true);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(modifierGroupRepository.findByMenuItemId(10L, true)).thenReturn(List.of(group));
        when(modifierOptionRepository.findById(10L)).thenReturn(Optional.of(extra));
        lenient().when(transactionRepository.update(tx)).thenReturn(tx);

        service.updateItem(1L, 20L, null, List.of(10L), "no sugar");

        assertEquals(0, new BigDecimal("23000.00").compareTo(line.getUnitPrice()));
        assertEquals(1, line.getModifiers().size());
        assertEquals("no sugar", line.getNote());
    }

    @Test
    void addItemMergeRepricesAtTheCurrentOptionPrice() {
        Transaction tx = draft(1L);
        ModifierGroup group = group(5L, "Extras", SelectionType.MULTI, false, 0, 3);
        ModifierOption extra = option(10L, group, "Extra shot", "5000", true);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem(10L, "18000.00", true)));
        when(modifierGroupRepository.findByMenuItemId(10L, true)).thenReturn(List.of(group));
        when(modifierOptionRepository.findById(10L)).thenReturn(Optional.of(extra));
        lenient().when(transactionRepository.update(tx)).thenReturn(tx);

        service.addItem(1L, 10L, 1, List.of(10L), null);
        extra.setPriceDelta(new BigDecimal("9000.00"));
        service.addItem(1L, 10L, 1, List.of(10L), null);

        TransactionItem line = tx.getItems().get(0);
        assertEquals(2, line.getQuantity());
        assertEquals(0, new BigDecimal("27000.00").compareTo(line.getUnitPrice()));
        assertEquals(0, new BigDecimal("54000.00").compareTo(line.getTotalPrice()));
        assertEquals(0, new BigDecimal("9000.00").compareTo(
                line.getModifiers().get(0).getPriceDelta()));
    }

    @Test
    void updateItemRejectsADuplicateLineIdentity() {
        Transaction tx = draft(1L);
        ModifierGroup temperature = group(5L, "Temperature", SelectionType.SINGLE, true, 1, 1);
        ModifierOption hot = option(1L, temperature, "Hot", "0", true);
        ModifierOption iced = option(2L, temperature, "Iced", "0", true);

        TransactionItem hotLine = line("18000.00", 1);
        hotLine.setId(10L);
        hotLine.setNote("no sugar");
        hotLine.addModifier(snapshot(hot, 0));
        TransactionItem icedLine = line("18000.00", 1);
        icedLine.setId(11L);
        icedLine.setNote("no sugar");
        icedLine.addModifier(snapshot(iced, 0));
        tx.addItem(hotLine);
        tx.addItem(icedLine);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(modifierGroupRepository.findByMenuItemId(10L, true))
                .thenReturn(List.of(temperature));
        when(modifierOptionRepository.findById(1L)).thenReturn(Optional.of(hot));

        assertThrows(BusinessRuleException.class,
                () -> service.updateItem(1L, 11L, null, List.of(1L), null));
    }

    @Test
    void updateItemLeavesOptionsWhenNullAndClearsOnEmptyList() {
        Transaction tx = draft(1L);
        ModifierGroup group = group(5L, "Extras", SelectionType.MULTI, false, 0, 3);
        ModifierOption extra = option(10L, group, "Extra shot", "5000", true);
        TransactionItem line = line("23000.00", 1);
        line.setId(20L);
        line.addModifier(snapshot(extra, 0));
        tx.addItem(line);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(modifierGroupRepository.findByMenuItemId(10L, true)).thenReturn(List.of(group));
        lenient().when(transactionRepository.update(tx)).thenReturn(tx);

        service.updateItem(1L, 20L, null, null, "still hot");
        assertEquals(1, line.getModifiers().size());
        assertEquals("still hot", line.getNote());

        service.updateItem(1L, 20L, null, List.of(), null);
        assertEquals(0, line.getModifiers().size());
        assertEquals(0, new BigDecimal("23000.00").compareTo(line.getUnitPrice()));
        assertEquals("still hot", line.getNote());
    }

    @Test
    void addItemNeverMergesIntoALineWhoseOptionWasDeleted() {
        Transaction tx = draft(1L);
        TransactionItem existing = line("18000.00", 1);
        existing.setId(20L);
        TransactionItemModifier orphaned = new TransactionItemModifier();
        orphaned.setModifierOptionId(null);
        orphaned.setGroupName("Temperature");
        orphaned.setOptionName("Hot");
        orphaned.setPriceDelta(BigDecimal.ZERO);
        orphaned.setSortOrder(0);
        existing.addModifier(orphaned);
        tx.addItem(existing);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem(10L, "18000.00", true)));
        lenient().when(transactionRepository.update(tx)).thenReturn(tx);

        service.addItem(1L, 10L, 1, null, null);

        assertEquals(2, tx.getItems().size());
    }

    private TransactionItemModifier snapshot(ModifierOption option, int sortOrder) {
        TransactionItemModifier snapshot = new TransactionItemModifier();
        snapshot.setModifierOptionId(option.getId());
        snapshot.setGroupName(option.getGroup().getName());
        snapshot.setOptionName(option.getName());
        snapshot.setPriceDelta(option.getPriceDelta());
        snapshot.setSortOrder(sortOrder);
        return snapshot;
    }

    private Transaction settledBill() {
        Transaction tx = draft(1L);
        tx.setStatus(Transaction.Status.COMPLETED);
        tx.setPaymentMethod("CASH");
        TransactionItem line = line("100.00", 2);
        line.setId(10L);
        tx.addItem(line);
        tx.setSubtotalAmount(new BigDecimal("200.00"));
        tx.setChargesAmount(new BigDecimal("20.00"));
        tx.setTotalAmount(new BigDecimal("220.00"));
        return tx;
    }
}
