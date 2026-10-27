package com.anhilmy.pos.dto;

import java.math.BigDecimal;

/** Create/update payload for an option inside a modifier group. */
public class ModifierOptionRequest {
    public String name;
    public BigDecimal priceDelta;
    public Boolean isDefault;
    public Boolean available;
    public Integer sortOrder;
}
