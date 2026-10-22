package com.anhilmy.pos.dto;

import java.math.BigDecimal;

/** Create/update payload for a menu item. */
public class MenuItemRequest {
    public String name;
    public String description;
    public BigDecimal price;
    public Long categoryId;
    public Boolean available;
    public String imageUrl;
}
