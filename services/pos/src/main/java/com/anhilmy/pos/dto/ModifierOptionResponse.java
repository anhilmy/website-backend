package com.anhilmy.pos.dto;

import java.math.BigDecimal;

import com.anhilmy.pos.model.ModifierOption;

/** Wire representation of an option (no JPA back-references). */
public class ModifierOptionResponse {
    public Long id;
    public Long groupId;
    public String name;
    public BigDecimal priceDelta;
    public Boolean isDefault;
    public Boolean available;
    public Integer sortOrder;

    public static ModifierOptionResponse from(ModifierOption option) {
        ModifierOptionResponse response = new ModifierOptionResponse();
        response.id = option.getId();
        response.groupId = option.getGroup() == null ? null : option.getGroup().getId();
        response.name = option.getName();
        response.priceDelta = option.getPriceDelta();
        response.isDefault = option.getIsDefault();
        response.available = option.getAvailable();
        response.sortOrder = option.getSortOrder();
        return response;
    }
}
