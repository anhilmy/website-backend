package com.anhilmy.pos.model;

/** The base a charge is calculated on. */
public enum ChargeBasis {
    /** Line totals only. */
    SUBTOTAL,
    /** Subtotal plus earlier charges marked taxable. */
    RUNNING_TOTAL
}
