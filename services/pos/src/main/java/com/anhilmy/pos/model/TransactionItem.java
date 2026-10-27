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
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "transaction_items")
public class TransactionItem {

        /** Voided lines stay on the bill for audit but are excluded from totals. */
        public enum Status {
                ACTIVE, VOIDED
        }

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(optional = false)
        @JoinColumn(name = "transaction_id", nullable = false)
        private Transaction transaction;

        @ManyToOne(optional = false)
        @JoinColumn(name = "menu_item_id", nullable = false)
        private MenuItem menuItem;

        @NotNull
        @Positive
        @Column(nullable = false)
        private Integer quantity;

        @NotNull
        @Positive
        @Column(name = "unit_price", nullable = false)
        private BigDecimal unitPrice;

        @NotNull
        @Positive
        @Column(name = "total_price", nullable = false)
        private BigDecimal totalPrice;

        @NotNull
        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 16)
        private Status status = Status.ACTIVE;

        @Column(name = "void_reason", length = 255)
        private String voidReason;

        @Column(name = "voided_by", length = 255)
        private String voidedBy;

        @Column(name = "voided_at")
        private LocalDateTime voidedAt;

        public TransactionItem() {}

        // Getters and setters
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

        public MenuItem getMenuItem() {
                return menuItem;
        }

        public void setMenuItem(MenuItem menuItem) {
                this.menuItem = menuItem;
        }

        public Integer getQuantity() {
                return quantity;
        }

        public void setQuantity(Integer quantity) {
                this.quantity = quantity;
        }

        public BigDecimal getUnitPrice() {
                return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
                this.unitPrice = unitPrice;
        }

        public BigDecimal getTotalPrice() {
                return totalPrice;
        }

        public void setTotalPrice(BigDecimal totalPrice) {
                this.totalPrice = totalPrice;
        }

        public Status getStatus() {
                return status;
        }

        public void setStatus(Status status) {
                this.status = status;
        }

        public String getVoidReason() {
                return voidReason;
        }

        public void setVoidReason(String voidReason) {
                this.voidReason = voidReason;
        }

        public String getVoidedBy() {
                return voidedBy;
        }

        public void setVoidedBy(String voidedBy) {
                this.voidedBy = voidedBy;
        }

        public LocalDateTime getVoidedAt() {
                return voidedAt;
        }

        public void setVoidedAt(LocalDateTime voidedAt) {
                this.voidedAt = voidedAt;
        }
}
