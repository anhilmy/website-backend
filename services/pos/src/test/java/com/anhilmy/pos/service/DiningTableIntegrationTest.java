package com.anhilmy.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.anhilmy.pos.db.DbManager;
import com.anhilmy.pos.dto.TableRequest;
import com.anhilmy.pos.model.DiningTable;
import com.anhilmy.pos.model.Roles;
import com.anhilmy.pos.model.TableStatus;
import com.anhilmy.pos.model.Transaction;
import com.anhilmy.pos.model.User;
import com.anhilmy.pos.repository.TransactionRepository;
import com.anhilmy.pos.repository.UserRepository;
import com.anhilmy.pos.util.PasswordUtil;

/** Floor-table lifecycle against a real (SQLite) database. */
class DiningTableIntegrationTest {

    private final UserRepository userRepository = new UserRepository();
    private final TransactionRepository transactionRepository = new TransactionRepository();
    private final DiningTableService tableService = new DiningTableService();

    private Long cashierId;

    @BeforeEach
    void openSession() {
        DbManager.openSession();
        User cashier = new User();
        cashier.setUsername("tbl_" + System.nanoTime());
        cashier.setPasswordHash(PasswordUtil.hash("password1"));
        cashier.setRole(Roles.CASHIER);
        cashier.setCreatedAt(LocalDateTime.now());
        cashier.setUpdatedAt(LocalDateTime.now());
        userRepository.save(cashier);
        cashierId = cashier.getId();
    }

    @AfterEach
    void closeSession() {
        if (DbManager.hasSession()) {
            DbManager.commitAndClose();
        }
    }

    private DiningTable newTable(String name, String area) {
        TableRequest request = new TableRequest();
        request.name = name;
        request.area = area;
        return tableService.create(request);
    }

    @Test
    void openJoinAndReleaseFlow() {
        String suffix = Long.toString(System.nanoTime());
        DiningTable t1 = newTable("A" + suffix, "Indoor");
        DiningTable t2 = newTable("B" + suffix, "Indoor");

        assertEquals(TableStatus.AVAILABLE, t1.getStatus());

        // Open t1 -> creates a draft order and occupies the table.
        DiningTable opened = tableService.open(t1.getId(), cashierId, null);
        assertEquals(TableStatus.OCCUPIED, opened.getStatus());
        assertNotNull(opened.getCurrentTransaction());
        Long orderId = opened.getCurrentTransaction().getId();

        // Join t2 into t1 -> t2 shares t1's bill.
        DiningTable joined = tableService.join(t2.getId(), t1.getId());
        assertEquals(TableStatus.JOINED, joined.getStatus());
        assertEquals(t1.getId(), joined.getJoinedTo().getId());
        assertEquals(orderId, joined.getCurrentTransaction().getId());

        // Release the host -> both tables free, bill settled/cancelled.
        DiningTable released = tableService.release(t1.getId());
        assertEquals(TableStatus.CLEANING, released.getStatus());
        assertNull(released.getCurrentTransaction());

        // The open bill is explicitly cancelled, never silently orphaned.
        assertEquals(Transaction.Status.CANCELLED, transactionRepository.findById(orderId).orElseThrow().getStatus());

        DiningTable releasedChild = tableService.getById(t2.getId());
        assertEquals(TableStatus.AVAILABLE, releasedChild.getStatus());
        assertNull(releasedChild.getJoinedTo());
        assertNull(releasedChild.getCurrentTransaction());
    }

    @Test
    void openingHostPropagatesBillToJoinedTables() {
        String suffix = Long.toString(System.nanoTime());
        DiningTable host = newTable("H" + suffix, "Indoor");
        DiningTable guest = newTable("G" + suffix, "Indoor");

        // Join while both are empty, then open the host.
        tableService.join(guest.getId(), host.getId());
        DiningTable opened = tableService.open(host.getId(), cashierId, null);
        Long billId = opened.getCurrentTransaction().getId();

        DiningTable guestAfter = tableService.getById(guest.getId());
        assertEquals(TableStatus.JOINED, guestAfter.getStatus());
        assertNotNull(guestAfter.getCurrentTransaction());
        assertEquals(billId, guestAfter.getCurrentTransaction().getId());
    }

    @Test
    void tableListFiltersByArea() {
        String area = "Patio_" + System.nanoTime();
        newTable("P1_" + System.nanoTime(), area);
        newTable("P2_" + System.nanoTime(), area);

        assertEquals(2, tableService.list(area, null).size());
        assertEquals(2, tableService.list(area, TableStatus.AVAILABLE).size());
        assertEquals(0, tableService.list(area, TableStatus.OCCUPIED).size());
    }
}
