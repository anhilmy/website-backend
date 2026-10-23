package com.anhilmy.pos.dto;

import java.math.BigDecimal;

import com.anhilmy.pos.model.DiningTable;

/**
 * Safe projection of a {@link DiningTable}.
 *
 * <p>Deliberately avoids embedding the full {@link com.anhilmy.pos.model.Transaction} (customer
 * PII, line items) and the self-referencing {@code joinedTo} entity, which risks unbounded
 * recursion if a join cycle ever exists.
 */
public class TableResponse {

    private final Long id;
    private final String name;
    private final String area;
    private final String status;
    private final Long currentTransactionId;
    private final BigDecimal currentTransactionTotal;
    private final Long joinedToId;
    private final String joinedToName;

    private TableResponse(Long id, String name, String area, String status, Long currentTransactionId,
            BigDecimal currentTransactionTotal, Long joinedToId, String joinedToName) {
        this.id = id;
        this.name = name;
        this.area = area;
        this.status = status;
        this.currentTransactionId = currentTransactionId;
        this.currentTransactionTotal = currentTransactionTotal;
        this.joinedToId = joinedToId;
        this.joinedToName = joinedToName;
    }

    public static TableResponse from(DiningTable t) {
        if (t == null) {
            return null;
        }
        Long txId = t.getCurrentTransaction() == null ? null : t.getCurrentTransaction().getId();
        BigDecimal total = t.getCurrentTransaction() == null ? null
                : t.getCurrentTransaction().getTotalAmount();
        Long hostId = t.getJoinedTo() == null ? null : t.getJoinedTo().getId();
        String hostName = t.getJoinedTo() == null ? null : t.getJoinedTo().getName();
        return new TableResponse(t.getId(), t.getName(), t.getArea(),
                t.getStatus() == null ? null : t.getStatus().name(), txId, total, hostId, hostName);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getArea() {
        return area;
    }

    public String getStatus() {
        return status;
    }

    public Long getCurrentTransactionId() {
        return currentTransactionId;
    }

    public BigDecimal getCurrentTransactionTotal() {
        return currentTransactionTotal;
    }

    public Long getJoinedToId() {
        return joinedToId;
    }

    public String getJoinedToName() {
        return joinedToName;
    }
}
