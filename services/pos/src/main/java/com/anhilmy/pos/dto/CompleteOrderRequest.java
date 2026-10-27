package com.anhilmy.pos.dto;

import java.math.BigDecimal;

/** Complete-order payload (payment details). */
public class CompleteOrderRequest {
    public String paymentMethod;
    public BigDecimal paymentAmount;
    public String paymentReference;
}
