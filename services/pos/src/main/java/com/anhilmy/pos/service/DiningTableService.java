package com.anhilmy.pos.service;

import java.time.LocalDateTime;
import java.util.List;

import com.anhilmy.pos.dto.TableRequest;
import com.anhilmy.pos.exception.BusinessRuleException;
import com.anhilmy.pos.exception.EntityNotFoundException;
import com.anhilmy.pos.model.DiningTable;
import com.anhilmy.pos.model.TableStatus;
import com.anhilmy.pos.model.Transaction;
import com.anhilmy.pos.repository.DiningTableRepository;
import com.anhilmy.pos.repository.TransactionRepository;
import com.anhilmy.pos.util.InputValidator;

/**
 * Floor-table management: occupancy, attaching an order, and merging tables.
 *
 * <p>A table may be <em>joined</em> into another (host) table; the joined table then shares the
 * host's {@code currentTransaction}, so the party settles on a single bill.
 *
 * <p>Invariants enforced here:
 * <ul>
 * <li>{@code status == JOINED} ⇔ {@code joinedTo != null}</li>
 * <li>{@code status == OCCUPIED} ⇒ {@code currentTransaction != null}</li>
 * <li>a bill is never silently detached: it is settled/cancelled explicitly or refused</li>
 * <li>{@code JOINED} is only produced by {@link #join}, never by a raw status change</li>
 * </ul>
 */
public class DiningTableService {

    private static final int MAX_NAME = 100;
    private static final int MAX_AREA = 100;

    private final DiningTableRepository tableRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    public DiningTableService() {
        this(new DiningTableRepository(), new TransactionRepository(), new TransactionService());
    }

    public DiningTableService(DiningTableRepository tableRepository,
            TransactionRepository transactionRepository, TransactionService transactionService) {
        this.tableRepository = tableRepository;
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
    }

    public List<DiningTable> list(String area, TableStatus status) {
        return tableRepository.findFiltered(area, status);
    }

    public DiningTable getById(Long id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DiningTable", id));
    }

    public DiningTable create(TableRequest request) {
        String name = InputValidator.requireMaxLength(request.name, "name", MAX_NAME);
        String area = InputValidator.requireMaxLength(request.area, "area", MAX_AREA);
        tableRepository.findByName(name).ifPresent(t -> {
            throw new BusinessRuleException("Table name already exists: " + name);
        });
        DiningTable table = new DiningTable();
        table.setName(name);
        table.setArea(area);
        table.setStatus(TableStatus.AVAILABLE);
        table.setCreatedAt(LocalDateTime.now());
        table.setUpdatedAt(LocalDateTime.now());
        return tableRepository.save(table);
    }

    public DiningTable update(Long id, TableRequest request) {
        DiningTable table = getById(id);
        if (request.name != null) {
            String name = InputValidator.requireMaxLength(request.name, "name", MAX_NAME);
            tableRepository.findByName(name).ifPresent(other -> {
                if (!other.getId().equals(id)) {
                    throw new BusinessRuleException("Table name already exists: " + name);
                }
            });
            table.setName(name);
        }
        if (request.area != null) {
            table.setArea(InputValidator.requireMaxLength(request.area, "area", MAX_AREA));
        }
        touch(table);
        return tableRepository.update(table);
    }

    /** Deleting is blocked while the table is in use or hosting joined tables. */
    public void delete(Long id) {
        DiningTable table = getById(id);
        if (table.getStatus() == TableStatus.OCCUPIED || table.getStatus() == TableStatus.JOINED) {
            throw new BusinessRuleException("Cannot delete a table that is in use");
        }
        if (table.getCurrentTransaction() != null) {
            throw new BusinessRuleException("Cannot delete a table with an open bill");
        }
        if (!tableRepository.findJoinedTo(id).isEmpty()) {
            throw new BusinessRuleException("Cannot delete a table with joined tables");
        }
        tableRepository.delete(table);
    }

    /**
     * Manual status change. {@code JOINED} is rejected here (only {@link #join} may produce it),
     * and a table holding a bill can only be freed through {@link #release}.
     */
    public DiningTable setStatus(Long id, TableStatus next) {
        DiningTable table = getById(id);
        TableStatus current = table.getStatus();
        if (next == TableStatus.JOINED) {
            throw new BusinessRuleException("Use join to merge tables, not a status change");
        }
        if (!current.canTransitionTo(next)) {
            throw new BusinessRuleException(
                    "Cannot move table from " + current + " to " + next);
        }
        if (next == TableStatus.OCCUPIED && table.getCurrentTransaction() == null) {
            throw new BusinessRuleException("Open or assign an order before marking a table occupied");
        }
        if (next == TableStatus.AVAILABLE && table.getCurrentTransaction() != null) {
            throw new BusinessRuleException(
                    "Table has an open bill; release it before marking the table available");
        }
        table.setStatus(next);
        if (next == TableStatus.AVAILABLE) {
            table.setJoinedTo(null);
        }
        touch(table);
        return tableRepository.update(table);
    }

    /** Opens a table: creates a draft order for the cashier and attaches it. */
    public DiningTable open(Long id, Long cashierId, String customerInfo) {
        DiningTable table = getById(id);
        requireFree(table);
        Transaction order = transactionService.createDraft(
                cashierId, customerInfo != null ? customerInfo : table.getName());
        table.setCurrentTransaction(order);
        table.setStatus(TableStatus.OCCUPIED);
        touch(table);
        DiningTable saved = tableRepository.update(table);
        propagateBill(saved);
        return saved;
    }

    /**
     * Attaches an existing open order to the table. Only {@code DRAFT} orders can be seated, and
     * an order cannot be on two tables at once.
     */
    public DiningTable assign(Long id, Long transactionId) {
        DiningTable table = getById(id);
        if (table.getStatus() == TableStatus.JOINED
                || table.getStatus() == TableStatus.OUT_OF_SERVICE
                || table.getStatus() == TableStatus.CLEANING) {
            throw new BusinessRuleException("Cannot assign an order to a " + table.getStatus()
                    + " table");
        }
        Transaction order = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction", transactionId));
        if (order.getStatus() != Transaction.Status.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT orders can be seated (order is " + order.getStatus() + ")");
        }
        if (table.getCurrentTransaction() != null
                && !table.getCurrentTransaction().getId().equals(transactionId)) {
            throw new BusinessRuleException(
                    "Table already has order #" + table.getCurrentTransaction().getId());
        }
        tableRepository.findByTransactionId(transactionId).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new BusinessRuleException(
                        "Order #" + transactionId + " is already seated at table " + other.getName());
            }
        });
        table.setCurrentTransaction(order);
        table.setStatus(TableStatus.OCCUPIED);
        touch(table);
        DiningTable saved = tableRepository.update(table);
        propagateBill(saved);
        return saved;
    }

    /**
     * Closes the table: cancels any still-open bill, frees joined tables, and marks it for
     * cleaning. A joined table must be un-joined before it can be released on its own.
     */
    public DiningTable release(Long id) {
        DiningTable table = getById(id);
        if (table.getJoinedTo() != null) {
            throw new BusinessRuleException(
                    "Unjoin " + table.getName() + " from " + table.getJoinedTo().getName()
                            + " before releasing it");
        }
        cancelOpenBill(table);
        for (DiningTable joined : tableRepository.findJoinedTo(id)) {
            joined.setJoinedTo(null);
            joined.setCurrentTransaction(null);
            joined.setStatus(TableStatus.AVAILABLE);
            touch(joined);
            tableRepository.update(joined);
        }
        table.setCurrentTransaction(null);
        table.setStatus(TableStatus.CLEANING);
        touch(table);
        return tableRepository.update(table);
    }

    /** Merges {@code id} into {@code targetId}; both then share one bill. */
    public DiningTable join(Long id, Long targetId) {
        if (targetId == null) {
            throw new BusinessRuleException("targetTableId is required");
        }
        if (id.equals(targetId)) {
            throw new BusinessRuleException("A table cannot be joined to itself");
        }
        DiningTable table = getById(id);
        DiningTable target = getById(targetId);

        if (table.getStatus() == TableStatus.OUT_OF_SERVICE
                || table.getStatus() == TableStatus.CLEANING
                || target.getStatus() == TableStatus.OUT_OF_SERVICE
                || target.getStatus() == TableStatus.CLEANING) {
            throw new BusinessRuleException("Out-of-service or cleaning tables cannot be joined");
        }
        if (table.getJoinedTo() != null) {
            throw new BusinessRuleException("Table " + table.getName() + " is already joined");
        }
        if (target.getJoinedTo() != null) {
            throw new BusinessRuleException(
                    "Cannot join into a joined table; join into its host instead");
        }

        boolean tableHasOwnBill = table.getCurrentTransaction() != null;
        boolean targetHasBill = target.getCurrentTransaction() != null;
        if (tableHasOwnBill && targetHasBill
                && !table.getCurrentTransaction().getId()
                        .equals(target.getCurrentTransaction().getId())) {
            throw new BusinessRuleException(
                    "Both tables have separate orders; release one before joining");
        }
        // If only the joining table has a bill, the host adopts it.
        if (tableHasOwnBill && !targetHasBill) {
            target.setCurrentTransaction(table.getCurrentTransaction());
            target.setStatus(TableStatus.OCCUPIED);
            touch(target);
            tableRepository.update(target);
        }

        table.setJoinedTo(target);
        table.setCurrentTransaction(target.getCurrentTransaction());
        table.setStatus(TableStatus.JOINED);
        touch(table);
        return tableRepository.update(table);
    }

    /** Detaches a joined table from its host. */
    public DiningTable unjoin(Long id) {
        DiningTable table = getById(id);
        if (table.getJoinedTo() == null) {
            throw new BusinessRuleException("Table is not joined to another");
        }
        table.setJoinedTo(null);
        table.setCurrentTransaction(null);
        table.setStatus(TableStatus.AVAILABLE);
        touch(table);
        return tableRepository.update(table);
    }

    /**
     * Frees the table (host, if the bill is shared) seating the given bill. Called when an order
     * is settled or cancelled so the floor never keeps a finished bill sitting on a table.
     */
    public void releaseSeatedTable(Long transactionId) {
        if (transactionId == null) {
            return;
        }
        tableRepository.findByTransactionId(transactionId).ifPresent(seated -> {
            DiningTable host = seated.getJoinedTo() != null ? seated.getJoinedTo() : seated;
            release(host.getId());
        });
    }

    /** Cancels a DRAFT bill so releasing a table never leaves an orphaned order behind. */
    private void cancelOpenBill(DiningTable table) {
        Transaction bill = table.getCurrentTransaction();
        if (bill != null && bill.getStatus() == Transaction.Status.DRAFT) {
            bill.setStatus(Transaction.Status.CANCELLED);
            bill.setUpdatedAt(LocalDateTime.now());
            transactionRepository.update(bill);
        }
    }

    /** Keeps every table joined to this host pointing at the same bill. */
    private void propagateBill(DiningTable host) {
        for (DiningTable joined : tableRepository.findJoinedTo(host.getId())) {
            joined.setCurrentTransaction(host.getCurrentTransaction());
            touch(joined);
            tableRepository.update(joined);
        }
    }

    private void requireFree(DiningTable table) {
        if (table.getStatus() != TableStatus.AVAILABLE && table.getStatus() != TableStatus.RESERVED) {
            throw new BusinessRuleException(
                    "Table " + table.getName() + " cannot be opened while " + table.getStatus());
        }
    }

    private void touch(DiningTable table) {
        table.setUpdatedAt(LocalDateTime.now());
    }
}
