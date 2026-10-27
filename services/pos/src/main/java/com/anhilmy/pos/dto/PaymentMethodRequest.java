package com.anhilmy.pos.dto;

/** Create/update payload for a payment method. */
public class PaymentMethodRequest {
    public String code;
    public String label;
    public Boolean active;
    public Boolean isCash;
    public Boolean requiresReference;
    public Integer sortOrder;
}
