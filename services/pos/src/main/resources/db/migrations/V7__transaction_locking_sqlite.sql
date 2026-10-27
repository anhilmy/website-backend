-- V7 (SQLite): optimistic locking for bills and a one-amend-per-bill guarantee.
-- Apply with: sqlite3 pos_dev.db < V7__transaction_locking_sqlite.sql

ALTER TABLE transactions ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS uq_transactions_amended_from
    ON transactions (amended_from_id) WHERE amended_from_id IS NOT NULL;

INSERT INTO schema_migrations (version, description)
VALUES ('V7', 'transaction locking (sqlite)')
ON CONFLICT (version) DO NOTHING;
