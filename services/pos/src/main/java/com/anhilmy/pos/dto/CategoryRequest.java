package com.anhilmy.pos.dto;

/** Create/update payload for a menu category. */
public class CategoryRequest {
    public String name;
    public String description;
    public Boolean active;
    public Long parentId;
}
