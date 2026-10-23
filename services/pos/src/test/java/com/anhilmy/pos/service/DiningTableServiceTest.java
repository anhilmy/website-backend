package com.anhilmy.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anhilmy.pos.dto.TableRequest;
import com.anhilmy.pos.exception.BusinessRuleException;
import com.anhilmy.pos.model.DiningTable;
import com.anhilmy.pos.model.TableStatus;
import com.anhilmy.pos.model.Transaction;
import com.anhilmy.pos.repository.DiningTableRepository;
import com.anhilmy.pos.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class DiningTableServiceTest {

    @Mock
    private DiningTableRepository tableRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionService transactionService;

    private DiningTableService service;

    @BeforeEach
    void setUp() {
        service = new DiningTableService(tableRepository, transactionRepository, transactionService);
        lenient().when(tableRepository.update(any(DiningTable.class))).thenAnswer(i -> i.getArgument(0));
    }

    private DiningTable table(long id, String name, String area) {
        DiningTable t = new DiningTable();
        t.setId(id);
        t.setName(name);
        t.setArea(area);
        t.setStatus(TableStatus.AVAILABLE);
        return t;
    }

    private Transaction bill(long id) {
        Transaction tx = new Transaction();
        tx.setId(id);
        return tx;
    }

    @Test
    void joinPromotesSourceBillToHost() {
        DiningTable source = table(1L, "T1", "Indoor");
        source.setCurrentTransaction(bill(9L));
        DiningTable host = table(2L, "T2", "Indoor");
        when(tableRepository.findById(1L)).thenReturn(Optional.of(source));
        when(tableRepository.findById(2L)).thenReturn(Optional.of(host));

        DiningTable joined = service.join(1L, 2L);

        assertEquals(TableStatus.JOINED, joined.getStatus());
        assertSame(host, joined.getJoinedTo());
        assertEquals(9L, joined.getCurrentTransaction().getId());
        // The host adopted the bill and became occupied.
        assertEquals(9L, host.getCurrentTransaction().getId());
        assertEquals(TableStatus.OCCUPIED, host.getStatus());
    }

    @Test
    void joinRejectsSelf() {
        assertThrows(BusinessRuleException.class, () -> service.join(1L, 1L));
    }

    @Test
    void joinRejectsAlreadyJoinedSource() {
        DiningTable source = table(1L, "T1", "Indoor");
        source.setJoinedTo(table(3L, "T3", "Indoor"));
        DiningTable host = table(2L, "T2", "Indoor");
        when(tableRepository.findById(1L)).thenReturn(Optional.of(source));
        when(tableRepository.findById(2L)).thenReturn(Optional.of(host));

        assertThrows(BusinessRuleException.class, () -> service.join(1L, 2L));
    }

    @Test
    void joinRejectsConflictingBills() {
        DiningTable source = table(1L, "T1", "Indoor");
        source.setCurrentTransaction(bill(9L));
        DiningTable host = table(2L, "T2", "Indoor");
        host.setCurrentTransaction(bill(10L));
        when(tableRepository.findById(1L)).thenReturn(Optional.of(source));
        when(tableRepository.findById(2L)).thenReturn(Optional.of(host));

        assertThrows(BusinessRuleException.class, () -> service.join(1L, 2L));
    }

    @Test
    void joinRejectsOutOfService() {
        DiningTable source = table(1L, "T1", "Indoor");
        DiningTable host = table(2L, "T2", "Indoor");
        host.setStatus(TableStatus.OUT_OF_SERVICE);
        when(tableRepository.findById(1L)).thenReturn(Optional.of(source));
        when(tableRepository.findById(2L)).thenReturn(Optional.of(host));

        assertThrows(BusinessRuleException.class, () -> service.join(1L, 2L));
    }

    @Test
    void releaseFreesJoinedTables() {
        DiningTable host = table(1L, "T2", "Indoor");
        host.setStatus(TableStatus.OCCUPIED);
        host.setCurrentTransaction(bill(9L));
        DiningTable child = table(2L, "T1", "Indoor");
        child.setStatus(TableStatus.JOINED);
        child.setJoinedTo(host);
        child.setCurrentTransaction(bill(9L));

        when(tableRepository.findById(1L)).thenReturn(Optional.of(host));
        when(tableRepository.findJoinedTo(1L)).thenReturn(List.of(child));

        DiningTable released = service.release(1L);

        assertEquals(TableStatus.CLEANING, released.getStatus());
        assertNull(released.getCurrentTransaction());
        assertEquals(TableStatus.AVAILABLE, child.getStatus());
        assertNull(child.getJoinedTo());
        assertNull(child.getCurrentTransaction());
    }

    @Test
    void setStatusRejectsIllegalTransition() {
        DiningTable t = table(1L, "T1", "Indoor"); // AVAILABLE
        when(tableRepository.findById(1L)).thenReturn(Optional.of(t));

        assertThrows(BusinessRuleException.class, () -> service.setStatus(1L, TableStatus.CLEANING));
    }

    @Test
    void setStatusRejectsJoined() {
        DiningTable t = table(1L, "T1", "Indoor");
        when(tableRepository.findById(1L)).thenReturn(Optional.of(t));

        // JOINED must only come from join(), never a raw status change.
        assertThrows(BusinessRuleException.class, () -> service.setStatus(1L, TableStatus.JOINED));
    }

    @Test
    void setStatusAvailableRefusesWhileBillOpen() {
        DiningTable t = table(1L, "T1", "Indoor");
        t.setStatus(TableStatus.OCCUPIED);
        t.setCurrentTransaction(bill(9L));
        when(tableRepository.findById(1L)).thenReturn(Optional.of(t));

        assertThrows(BusinessRuleException.class, () -> service.setStatus(1L, TableStatus.AVAILABLE));
    }

    @Test
    void releaseRefusesAJoinedChild() {
        DiningTable child = table(1L, "T1", "Indoor");
        child.setStatus(TableStatus.JOINED);
        child.setJoinedTo(table(2L, "T2", "Indoor"));
        when(tableRepository.findById(1L)).thenReturn(Optional.of(child));

        assertThrows(BusinessRuleException.class, () -> service.release(1L));
    }

    @Test
    void releaseCancelsTheOpenBill() {
        DiningTable t = table(1L, "T1", "Indoor");
        t.setStatus(TableStatus.OCCUPIED);
        Transaction open = bill(9L);
        open.setStatus(Transaction.Status.DRAFT);
        t.setCurrentTransaction(open);
        when(tableRepository.findById(1L)).thenReturn(Optional.of(t));
        when(tableRepository.findJoinedTo(1L)).thenReturn(List.of());

        service.release(1L);

        assertEquals(Transaction.Status.CANCELLED, open.getStatus());
    }

    @Test
    void assignRejectsNonDraftOrder() {
        DiningTable t = table(1L, "T1", "Indoor");
        when(tableRepository.findById(1L)).thenReturn(Optional.of(t));
        Transaction completed = bill(9L);
        completed.setStatus(Transaction.Status.COMPLETED);
        when(transactionRepository.findById(9L)).thenReturn(Optional.of(completed));

        assertThrows(BusinessRuleException.class, () -> service.assign(1L, 9L));
    }

    @Test
    void assignRejectsAnOrderAlreadySeatedElsewhere() {
        DiningTable t = table(1L, "T1", "Indoor");
        when(tableRepository.findById(1L)).thenReturn(Optional.of(t));
        Transaction draft = bill(9L);
        draft.setStatus(Transaction.Status.DRAFT);
        when(transactionRepository.findById(9L)).thenReturn(Optional.of(draft));
        when(tableRepository.findByTransactionId(9L)).thenReturn(Optional.of(table(2L, "T2", "Indoor")));

        assertThrows(BusinessRuleException.class, () -> service.assign(1L, 9L));
    }

    @Test
    void releaseSeatedTableFreesTheTable() {
        DiningTable host = table(1L, "T2", "Indoor");
        host.setStatus(TableStatus.OCCUPIED);
        host.setCurrentTransaction(bill(9L));
        when(tableRepository.findByTransactionId(9L)).thenReturn(Optional.of(host));
        when(tableRepository.findById(1L)).thenReturn(Optional.of(host));
        when(tableRepository.findJoinedTo(1L)).thenReturn(List.of());

        service.releaseSeatedTable(9L);

        assertEquals(TableStatus.CLEANING, host.getStatus());
        assertNull(host.getCurrentTransaction());
    }

    @Test
    void releaseSeatedTableFreesTheHostWhenSeatedTableIsJoined() {
        DiningTable host = table(1L, "T2", "Indoor");
        host.setStatus(TableStatus.OCCUPIED);
        host.setCurrentTransaction(bill(9L));
        DiningTable child = table(2L, "T1", "Indoor");
        child.setJoinedTo(host);
        child.setCurrentTransaction(bill(9L));
        when(tableRepository.findByTransactionId(9L)).thenReturn(Optional.of(child));
        when(tableRepository.findById(1L)).thenReturn(Optional.of(host));
        when(tableRepository.findJoinedTo(1L)).thenReturn(List.of(child));

        service.releaseSeatedTable(9L);

        assertEquals(TableStatus.CLEANING, host.getStatus());
        assertEquals(TableStatus.AVAILABLE, child.getStatus());
    }

    @Test
    void openAllowsOnlyAvailableOrReserved() {
        DiningTable t = table(1L, "T1", "Indoor");
        t.setStatus(TableStatus.CLEANING);
        when(tableRepository.findById(1L)).thenReturn(Optional.of(t));

        assertThrows(BusinessRuleException.class, () -> service.open(1L, 1L, null));
    }

    @Test
    void setStatusOccupiedRequiresAnOrder() {
        DiningTable t = table(1L, "T1", "Indoor");
        when(tableRepository.findById(1L)).thenReturn(Optional.of(t));

        assertThrows(BusinessRuleException.class, () -> service.setStatus(1L, TableStatus.OCCUPIED));
    }

    @Test
    void openRejectsOccupiedTable() {
        DiningTable t = table(1L, "T1", "Indoor");
        t.setStatus(TableStatus.OCCUPIED);
        t.setCurrentTransaction(bill(9L));
        when(tableRepository.findById(1L)).thenReturn(Optional.of(t));

        assertThrows(BusinessRuleException.class, () -> service.open(1L, 1L, null));
    }

    @Test
    void createRejectsDuplicateName() {
        when(tableRepository.findByName("T1")).thenReturn(Optional.of(table(1L, "T1", "Indoor")));
        TableRequest req = new TableRequest();
        req.name = "T1";
        req.area = "Indoor";

        assertThrows(BusinessRuleException.class, () -> service.create(req));
    }

    @Test
    void unjoinDetachesFromHost() {
        DiningTable t = table(1L, "T1", "Indoor");
        t.setStatus(TableStatus.JOINED);
        t.setJoinedTo(table(2L, "T2", "Indoor"));
        t.setCurrentTransaction(bill(9L));
        when(tableRepository.findById(1L)).thenReturn(Optional.of(t));

        DiningTable result = service.unjoin(1L);

        assertNull(result.getJoinedTo());
        assertNull(result.getCurrentTransaction());
        assertEquals(TableStatus.AVAILABLE, result.getStatus());
    }
}
