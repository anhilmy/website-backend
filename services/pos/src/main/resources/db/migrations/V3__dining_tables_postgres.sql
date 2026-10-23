-- V3 (PostgreSQL): dining tables (floor plan) with occupancy, version + table joining.
-- Apply with: psql -U <user> -d <db> -f V3__dining_tables_postgres.sql

CREATE TABLE IF NOT EXISTS dining_tables (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    area VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL DEFAULT 'AVAILABLE',
    current_transaction_id BIGINT,
    joined_to_id BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (current_transaction_id) REFERENCES transactions(id),
    FOREIGN KEY (joined_to_id) REFERENCES dining_tables(id)
);
CREATE INDEX IF NOT EXISTS idx_dining_tables_status ON dining_tables (status);
CREATE INDEX IF NOT EXISTS idx_dining_tables_area ON dining_tables (area);
CREATE UNIQUE INDEX IF NOT EXISTS uq_dining_tables_bill ON dining_tables (current_transaction_id);

INSERT INTO schema_migrations (version, description)
VALUES ('V3', 'dining tables (postgres)')
ON CONFLICT (version) DO NOTHING;
