package com.anhilmy.pos.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Reconciliation payload: one row per payment method plus a grand total. */
public class PaymentReport {
    public LocalDateTime from;
    public LocalDateTime to;
    public List<PaymentSummary> rows = new ArrayList<>();
    public PaymentSummary totals = new PaymentSummary("ALL");
}
