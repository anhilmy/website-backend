-- V1 baseline: bookkeeping table for the lightweight migration system.
-- Apply AFTER schema_sqlite.sql / schema_postgres.sql.

CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(50) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO schema_migrations (version, description) VALUES ('V1', 'baseline schema')
ON CONFLICT (version) DO NOTHING;
