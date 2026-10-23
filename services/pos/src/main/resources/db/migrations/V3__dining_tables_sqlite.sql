-- V3 (SQLite): dining tables (floor plan) with occupancy, version + table joining.
-- Apply with: sqlite3 pos_dev.db < V3__dining_tables_sqlite.sql

CREATE TABLE IF NOT EXISTS dining_tables (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    area TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'AVAILABLE',
    current_transaction_id INTEGER,
    joined_to_id INTEGER,
    version INTEGER NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (current_transaction_id) REFERENCES transactions(id),
    FOREIGN KEY (joined_to_id) REFERENCES dining_tables(id)
);
CREATE INDEX IF NOT EXISTS idx_dining_tables_status ON dining_tables (status);
CREATE INDEX IF NOT EXISTS idx_dining_tables_area ON dining_tables (area);
CREATE UNIQUE INDEX IF NOT EXISTS uq_dining_tables_bill ON dining_tables (current_transaction_id);

INSERT INTO schema_migrations (version, description)
VALUES ('V3', 'dining tables (sqlite)')
ON CONFLICT (version) DO NOTHING;
