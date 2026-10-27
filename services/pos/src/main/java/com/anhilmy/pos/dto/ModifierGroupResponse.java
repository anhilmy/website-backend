package com.anhilmy.pos.dto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.anhilmy.pos.model.ModifierGroup;
import com.anhilmy.pos.model.ModifierOption;

/** Wire representation of a modifier group plus its options. */
public class ModifierGroupResponse {
    public Long id;
    public Long menuItemId;
    public String name;
    public String selectionType;
    public Boolean required;
    public Integer minSelect;
    public Integer maxSelect;
    public Integer sortOrder;
    public Boolean active;
    public List<ModifierOptionResponse> options = new ArrayList<>();

    public static ModifierGroupResponse from(ModifierGroup group) {
        ModifierGroupResponse response = new ModifierGroupResponse();
        response.id = group.getId();
        response.menuItemId = group.getMenuItem() == null ? null : group.getMenuItem().getId();
        response.name = group.getName();
        response.selectionType = group.getSelectionType() == null
                ? null
                : group.getSelectionType().name();
        response.required = group.getRequired();
        response.minSelect = group.getMinSelect();
        response.maxSelect = group.getMaxSelect();
        response.sortOrder = group.getSortOrder();
        response.active = group.getActive();
        response.options = group.getOptions().stream()
                .sorted(Comparator
                        .comparingInt((ModifierOption o) -> o.getSortOrder() == null
                                ? 0
                                : o.getSortOrder())
                        .thenComparingLong(o -> o.getId() == null ? 0L : o.getId()))
                .map(ModifierOptionResponse::from)
                .toList();
        return response;
    }
}
