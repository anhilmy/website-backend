package com.anhilmy.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anhilmy.pos.exception.BusinessRuleException;
import com.anhilmy.pos.model.MenuItem;
import com.anhilmy.pos.model.Transaction;
import com.anhilmy.pos.model.TransactionItem;
import com.anhilmy.pos.model.User;
import com.anhilmy.pos.repository.MenuItemRepository;
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

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(transactionRepository, menuItemRepository, userRepository);
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
}
