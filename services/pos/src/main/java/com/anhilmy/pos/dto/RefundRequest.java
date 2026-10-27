package com.anhilmy.pos.dto;

/** Refund payload for a settled order line. */
public class RefundRequest {
    public Long transactionItemId;
    public Integer quantity;
    public String reason;
    public String paymentMethod;
}
