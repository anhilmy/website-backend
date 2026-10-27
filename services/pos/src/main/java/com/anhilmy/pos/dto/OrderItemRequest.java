package com.anhilmy.pos.dto;

import java.util.List;

/** Add/update payload for an order line. */
public class OrderItemRequest {
    public Long menuItemId;
    public Integer quantity;
    /** Selected option ids; null means "leave unchanged" on update, empty clears. */
    public List<Long> modifierOptionIds;
    /** Free-text line note; null means "leave unchanged" on update. */
    public String note;
}
