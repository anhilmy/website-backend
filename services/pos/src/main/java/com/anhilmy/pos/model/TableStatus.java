package com.anhilmy.pos.model;

/**
 * Floor-table lifecycle.
 *
 * <pre>
 * AVAILABLE ──► RESERVED ──► OCCUPIED ──► CLEANING ──► AVAILABLE
 *     │            │            │
 *     └────────────┴────────────┴──► JOINED ──► AVAILABLE   (join() / un-join)
 *     │
 *     └──► OUT_OF_SERVICE ──► AVAILABLE
 * </pre>
 *
 * <p>{@code JOINED} is excluded from {@link #canTransitionTo} targets: it may only be produced by
 * {@code DiningTableService.join()}, never by a raw status change (which would otherwise leave a
 * table marked joined with no host).
 */
public enum TableStatus {
    AVAILABLE, RESERVED, OCCUPIED, JOINED, CLEANING, OUT_OF_SERVICE;

    /** Whether a table may move from this status to {@code next} via a manual status change. */
    public boolean canTransitionTo(TableStatus next) {
        if (next == null) {
            return false;
        }
        if (next == this) {
            return true; // idempotent
        }
        return switch (this) {
            case AVAILABLE -> next == RESERVED || next == OUT_OF_SERVICE;
            case RESERVED -> next == AVAILABLE || next == OUT_OF_SERVICE;
            case OCCUPIED -> next == CLEANING;
            case JOINED -> next == AVAILABLE;
            case CLEANING -> next == AVAILABLE || next == OUT_OF_SERVICE;
            case OUT_OF_SERVICE -> next == AVAILABLE;
        };
    }
}
