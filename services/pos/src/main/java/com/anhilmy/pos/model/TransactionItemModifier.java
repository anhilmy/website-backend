package com.anhilmy.pos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * Snapshot of one selected option on an order line.
 *
 * <p>The group/option names and price delta are copied at order time so editing or deleting a
 * menu option never rewrites a historic bill. {@code modifierOptionId} is informational and may
 * become {@code null} if the option is deleted.
 */
@Entity
@Table(name = "transaction_item_modifiers")
public class TransactionItemModifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "transaction_item_id", nullable = false)
    private TransactionItem transactionItem;

    @Column(name = "modifier_option_id")
    private Long modifierOptionId;

    @NotNull
    @Column(name = "group_name", nullable = false, length = 64)
    private String groupName;

    @NotNull
    @Column(name = "option_name", nullable = false, length = 64)
    private String optionName;

    @NotNull
    @Column(name = "price_delta", nullable = false)
    private BigDecimal priceDelta = BigDecimal.ZERO;

    @NotNull
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public TransactionItemModifier() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TransactionItem getTransactionItem() {
        return transactionItem;
    }

    public void setTransactionItem(TransactionItem transactionItem) {
        this.transactionItem = transactionItem;
    }

    public Long getModifierOptionId() {
        return modifierOptionId;
    }

    public void setModifierOptionId(Long modifierOptionId) {
        this.modifierOptionId = modifierOptionId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getOptionName() {
        return optionName;
    }

    public void setOptionName(String optionName) {
        this.optionName = optionName;
    }

    public BigDecimal getPriceDelta() {
        return priceDelta;
    }

    public void setPriceDelta(BigDecimal priceDelta) {
        this.priceDelta = priceDelta;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
