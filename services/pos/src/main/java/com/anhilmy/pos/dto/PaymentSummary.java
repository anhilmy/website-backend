package com.anhilmy.pos.dto;

import java.math.BigDecimal;

/** Totals for one payment method (or the report grand total). */
public class PaymentSummary {
    public String method;
    public int count;
    public BigDecimal gross = BigDecimal.ZERO;
    public BigDecimal refunded = BigDecimal.ZERO;
    public BigDecimal net = BigDecimal.ZERO;

    public PaymentSummary(String method) {
        this.method = method;
    }

    /** One settled bill, counted against the method it was paid with. */
    public void addBill(BigDecimal grossAmount) {
        count++;
        gross = gross.add(grossAmount);
        net = gross.subtract(refunded);
    }

    /** Money paid back through this method (may differ from the sale method). */
    public void addRefund(BigDecimal refundedAmount) {
        refunded = refunded.add(refundedAmount);
        net = gross.subtract(refunded);
    }

    public void add(BigDecimal grossAmount, BigDecimal refundedAmount) {
        addBill(grossAmount);
        addRefund(refundedAmount);
    }
}
