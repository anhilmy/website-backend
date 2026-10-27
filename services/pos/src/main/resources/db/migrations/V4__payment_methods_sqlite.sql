-- V4 (SQLite): payment methods + payment reference on the bill.
-- Apply with: sqlite3 pos_dev.db < V4__payment_methods_sqlite.sql
-- For databases created before V4 only; a fresh baseline already includes both.

CREATE TABLE IF NOT EXISTS payment_methods (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL UNIQUE,
    label TEXT NOT NULL,
    active INTEGER NOT NULL DEFAULT 1,
    is_cash INTEGER NOT NULL DEFAULT 0,
    requires_reference INTEGER NOT NULL DEFAULT 0,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE transactions ADD COLUMN payment_reference TEXT;

CREATE INDEX IF NOT EXISTS idx_payment_methods_active ON payment_methods (active);

INSERT INTO schema_migrations (version, description)
VALUES ('V4', 'payment methods (sqlite)')
ON CONFLICT (version) DO NOTHING;
