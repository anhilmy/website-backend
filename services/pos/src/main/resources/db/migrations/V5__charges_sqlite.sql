-- V5 (SQLite): charge templates + order charge snapshots + persisted subtotal.
-- Apply with: sqlite3 pos_dev.db < V5__charges_sqlite.sql
-- For databases created before V5 only; a fresh baseline already includes these.

ALTER TABLE transactions ADD COLUMN subtotal_amount REAL NOT NULL DEFAULT 0;
ALTER TABLE transactions ADD COLUMN charges_amount REAL NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS charge_templates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL UNIQUE,
    label TEXT NOT NULL,
    calc_type TEXT NOT NULL DEFAULT 'PERCENTAGE',
    value REAL NOT NULL DEFAULT 0,
    basis TEXT NOT NULL DEFAULT 'SUBTOTAL',
    sort_order INTEGER NOT NULL DEFAULT 0,
    taxable INTEGER NOT NULL DEFAULT 0,
    is_tax INTEGER NOT NULL DEFAULT 0,
    active INTEGER NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS charges (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    transaction_id INTEGER NOT NULL,
    code TEXT NOT NULL,
    label TEXT NOT NULL,
    calc_type TEXT NOT NULL DEFAULT 'PERCENTAGE',
    value REAL NOT NULL DEFAULT 0,
    basis TEXT NOT NULL DEFAULT 'SUBTOTAL',
    sort_order INTEGER NOT NULL DEFAULT 0,
    taxable INTEGER NOT NULL DEFAULT 0,
    is_tax INTEGER NOT NULL DEFAULT 0,
    computed_amount REAL NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_charge_templates_active ON charge_templates (active);
CREATE INDEX IF NOT EXISTS idx_charges_transaction ON charges (transaction_id);

INSERT OR IGNORE INTO charge_templates
    (code, label, calc_type, value, basis, sort_order, taxable, is_tax, active)
VALUES ('TAX', 'Tax', 'PERCENTAGE', 0.10, 'SUBTOTAL', 10, 0, 1, 1);

INSERT INTO schema_migrations (version, description)
VALUES ('V5', 'charges (sqlite)')
ON CONFLICT (version) DO NOTHING;
