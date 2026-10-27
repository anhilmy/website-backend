package com.anhilmy.pos.model;

/** How a charge's amount is derived from its base. */
public enum ChargeCalcType {
    /** {@code value} is a rate, e.g. 0.10 for 10%. */
    PERCENTAGE,
    /** {@code value} is an absolute amount. */
    FIXED
}
