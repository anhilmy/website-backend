package com.anhilmy.pos.dto;

import com.anhilmy.pos.model.SelectionType;

/** Create/update payload for a modifier group. */
public class ModifierGroupRequest {
    public Long menuItemId;
    public String name;
    public SelectionType selectionType;
    public Boolean required;
    public Integer minSelect;
    public Integer maxSelect;
    public Integer sortOrder;
    public Boolean active;
}
