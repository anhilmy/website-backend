package com.anhilmy.pos.model;

import java.math.BigDecimal;

public class TransactionItem {
    private int transactionItemId;
    private int transactionId;
    private int itemId;
    private int quantity;
    private BigDecimal itemPrice;
    private BigDecimal totalPrice;

    // Getters and Setters
    public int getTransactionItemId() { return transactionItemId; }
    public void setTransactionItemId(int transactionItemId) { this.transactionItemId = transactionItemId; }

    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getItemPrice() { return itemPrice; }
    public void setItemPrice(BigDecimal itemPrice) { 
            if (itemPrice.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Price must be positive");

            this.itemPrice = itemPrice; 
    }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { 
        if (totalPrice.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Price must be positive");
    
        this.totalPrice = totalPrice; }
}