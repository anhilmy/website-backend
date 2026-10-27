-- V7 (PostgreSQL): optimistic locking for bills and a one-amend-per-bill guarantee.
-- Apply with: psql -U <user> -d <db> -f V7__transaction_locking_postgres.sql

ALTER TABLE transactions ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS uq_transactions_amended_from
    ON transactions (amended_from_id) WHERE amended_from_id IS NOT NULL;

INSERT INTO schema_migrations (version, description)
VALUES ('V7', 'transaction locking (postgres)')
ON CONFLICT (version) DO NOTHING;
