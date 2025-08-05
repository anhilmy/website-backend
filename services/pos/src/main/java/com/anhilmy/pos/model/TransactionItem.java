package com.anhilmy.pos.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        @Column(nullable = false)
        private BigDecimal unitPrice;

        @NotNull
        @Positive
        @Column(nullable = false)
        private BigDecimal totalPrice;

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
}
