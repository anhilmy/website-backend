package com.anhilmy.pos.dto;

import java.util.List;

/** Lightweight pagination envelope used by list endpoints. */
public class PageResponse<T> {

    private final List<T> items;
    private final int page;
    private final int size;
    private final long total;
    private final int totalPages;

    public PageResponse(List<T> items, int page, int size, long total) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.total = total;
        this.totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotal() {
        return total;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
