-- V6 (SQLite): line voids, append-only refunds and amendment links.
-- Apply with: sqlite3 pos_dev.db < V6__voids_refunds_amends_sqlite.sql
-- For databases created before V6 only; a fresh baseline already includes these.

ALTER TABLE transaction_items ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE transaction_items ADD COLUMN void_reason TEXT;
ALTER TABLE transaction_items ADD COLUMN voided_by TEXT;
ALTER TABLE transaction_items ADD COLUMN voided_at DATETIME;

ALTER TABLE transactions ADD COLUMN refunded_amount REAL NOT NULL DEFAULT 0;
ALTER TABLE transactions ADD COLUMN amended_from_id INTEGER;
ALTER TABLE transactions ADD COLUMN amended_to_id INTEGER;

CREATE TABLE IF NOT EXISTS refunds (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    transaction_id INTEGER NOT NULL,
    transaction_item_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    amount REAL NOT NULL DEFAULT 0,
    reason TEXT NOT NULL,
    payment_method TEXT,
    created_by TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    FOREIGN KEY (transaction_item_id) REFERENCES transaction_items(id)
);

CREATE INDEX IF NOT EXISTS idx_refunds_transaction ON refunds (transaction_id);
CREATE INDEX IF NOT EXISTS idx_refunds_item ON refunds (transaction_item_id);

INSERT INTO schema_migrations (version, description)
VALUES ('V6', 'voids, refunds, amends (sqlite)')
ON CONFLICT (version) DO NOTHING;
