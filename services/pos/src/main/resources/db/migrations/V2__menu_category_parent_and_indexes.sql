-- V2: lookup indexes (idempotent).
--
-- NOTE: the parent_id column is part of the baseline schema in schema_sqlite.sql /
-- schema_postgres.sql, so this migration deliberately does NOT re-add it (a duplicate
-- ADD COLUMN fails). Existing databases created before parent_id existed should apply the
-- guarded statement for their dialect below, once.

-- SQLite: apply ONLY if the column is missing (older databases):
--   ALTER TABLE menu_categories ADD COLUMN parent_id INTEGER;
-- PostgreSQL:
--   ALTER TABLE menu_categories ADD COLUMN IF NOT EXISTS parent_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_menu_categories_active ON menu_categories (active);
CREATE INDEX IF NOT EXISTS idx_menu_items_category ON menu_items (category_id);
CREATE INDEX IF NOT EXISTS idx_menu_items_available ON menu_items (available);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions (status);
CREATE INDEX IF NOT EXISTS idx_transactions_cashier ON transactions (cashier_id);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions (created_at);
CREATE INDEX IF NOT EXISTS idx_transaction_items_transaction ON transaction_items (transaction_id);

INSERT INTO schema_migrations (version, description)
VALUES ('V2', 'lookup indexes')
ON CONFLICT (version) DO NOTHING;
