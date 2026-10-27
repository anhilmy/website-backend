package com.anhilmy.pos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "transactions")
public class Transaction {

    public enum Status {
        DRAFT, IN_PROGRESS, COMPLETED, CANCELLED,
        PARTIALLY_REFUNDED, REFUNDED, AMENDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Optimistic lock: concurrent voids/refunds/amends must not double-apply. */
    @Version
    @Column(nullable = false)
    private Long version;

    @NotNull
    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** Sum of line totals; persisted so a settled bill can be reconciled without the lines. */
    @Column(name = "subtotal_amount", nullable = false)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    /** Sum of every evaluated charge. */
    @Column(name = "charges_amount", nullable = false)
    private BigDecimal chargesAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /** Populated on completion; null while the order is a draft. */
    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_amount")
    private BigDecimal paymentAmount;

    /** Card/QRIS reference or transfer note captured at settlement. */
    @Column(name = "payment_reference")
    private String paymentReference;

    /** Running aggregate of append-only refunds; the original total is never reduced. */
    @Column(name = "refunded_amount", nullable = false)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(name = "amended_from_id")
    private Long amendedFromId;

    @Column(name = "amended_to_id")
    private Long amendedToId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.DRAFT;

    @Column(name = "customer_info")
    private String customerInfo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @OrderBy("id ASC")
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Charge> charges = new ArrayList<>();

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Refund> refunds = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Transaction() {}

    /** Adds a line item and wires the back-reference. */
    public void addItem(TransactionItem item) {
        item.setTransaction(this);
        this.items.add(item);
    }

    public void removeItem(TransactionItem item) {
        this.items.remove(item);
        item.setTransaction(null);
    }

    /** Adds an order charge and wires the back-reference. */
    public void addCharge(Charge charge) {
        charge.setTransaction(this);
        this.charges.add(charge);
    }

    public void removeCharge(Charge charge) {
        this.charges.remove(charge);
        charge.setTransaction(null);
    }

    /** Adds a refund record and wires the back-reference. */
    public void addRefund(Refund refund) {
        refund.setTransaction(this);
        this.refunds.add(refund);
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public BigDecimal getChargesAmount() {
        return chargesAmount;
    }

    public void setChargesAmount(BigDecimal chargesAmount) {
        this.chargesAmount = chargesAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public void setRefundedAmount(BigDecimal refundedAmount) {
        this.refundedAmount = refundedAmount;
    }

    public Long getAmendedFromId() {
        return amendedFromId;
    }

    public void setAmendedFromId(Long amendedFromId) {
        this.amendedFromId = amendedFromId;
    }

    public Long getAmendedToId() {
        return amendedToId;
    }

    public void setAmendedToId(Long amendedToId) {
        this.amendedToId = amendedToId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getCustomerInfo() {
        return customerInfo;
    }

    public void setCustomerInfo(String customerInfo) {
        this.customerInfo = customerInfo;
    }

    public User getCashier() {
        return cashier;
    }

    public void setCashier(User cashier) {
        this.cashier = cashier;
    }

    public List<TransactionItem> getItems() {
        return items;
    }

    public void setItems(List<TransactionItem> items) {
        this.items = items;
    }

    public List<Charge> getCharges() {
        return charges;
    }

    public void setCharges(List<Charge> charges) {
        this.charges = charges;
    }

    public List<Refund> getRefunds() {
        return refunds;
    }

    public void setRefunds(List<Refund> refunds) {
        this.refunds = refunds;
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
