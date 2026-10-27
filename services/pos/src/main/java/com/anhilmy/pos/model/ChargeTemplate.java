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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * A reusable charge definition. Each new order copies the active templates into its own
 * {@link Charge} rows, so editing a template never rewrites a historic bill.
 */
@Entity
@Table(name = "charge_templates",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"code"})})
public class ChargeTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 32)
    private String code;

    @NotBlank
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

    /** Marks the charge that populates {@code Transaction.taxAmount}. */
    @Column(name = "is_tax", nullable = false)
    private Boolean isTax = Boolean.FALSE;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public ChargeTemplate() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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
