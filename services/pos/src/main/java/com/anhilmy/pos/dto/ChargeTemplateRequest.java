package com.anhilmy.pos.dto;

import java.math.BigDecimal;

import com.anhilmy.pos.model.ChargeBasis;
import com.anhilmy.pos.model.ChargeCalcType;

/** Create/update payload for a reusable charge template. */
public class ChargeTemplateRequest {
    public String code;
    public String label;
    public ChargeCalcType calcType;
    public BigDecimal value;
    public ChargeBasis basis;
    public Integer sortOrder;
    public Boolean taxable;
    public Boolean isTax;
    public Boolean active;
}
