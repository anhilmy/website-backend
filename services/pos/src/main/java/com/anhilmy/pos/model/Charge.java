package com.anhilmy.pos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * An order-level charge snapshot. Values are copied from the template at order creation so later
 * template edits cannot rewrite a historic bill.
 */
@Entity
@Table(name = "charges")
public class Charge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(nullable = false, length = 32)
    private String code;

    @NotNull
    @Column(nullable = false, length = 64)
    private String label;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "calc_type", nullable = false, length = 16)
    private ChargeCalcType calcType = ChargeCalcType.PERCENTAGE;

    @NotNull
    @Column(nullable = false)
    private BigDecimal value = BigDecimal.ZERO;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ChargeBasis basis = ChargeBasis.SUBTOTAL;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean taxable = Boolean.FALSE;

    @Column(name = "is_tax", nullable = false)
    private Boolean isTax = Boolean.FALSE;

    /** Persisted snapshot of the evaluated amount. */
    @NotNull
    @Column(name = "computed_amount", nullable = false)
    private BigDecimal computedAmount = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Charge() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public ChargeCalcType getCalcType() {
        return calcType;
    }

    public void setCalcType(ChargeCalcType calcType) {
        this.calcType = calcType;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public ChargeBasis getBasis() {
        return basis;
    }

    public void setBasis(ChargeBasis basis) {
        this.basis = basis;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getTaxable() {
        return taxable;
    }

    public void setTaxable(Boolean taxable) {
        this.taxable = taxable;
    }

    public Boolean getIsTax() {
        return isTax;
    }

    public void setIsTax(Boolean isTax) {
        this.isTax = isTax;
    }

    public BigDecimal getComputedAmount() {
        return computedAmount;
    }

    public void setComputedAmount(BigDecimal computedAmount) {
        this.computedAmount = computedAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
