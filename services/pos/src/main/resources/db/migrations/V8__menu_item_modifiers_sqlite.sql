-- V8 (SQLite): item modifier groups/options and snapshotted line modifiers.
-- Apply with: sqlite3 <db> < V8__menu_item_modifiers_sqlite.sql
-- For databases created before V8 only; a fresh baseline already includes these.
-- Do NOT apply to a database that was started with db.hbm2ddl=update: Hibernate has already
-- created the column/tables, so the ALTER below fails with "duplicate column name: note".

ALTER TABLE transaction_items ADD COLUMN note TEXT;

CREATE TABLE IF NOT EXISTS modifier_groups (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    menu_item_id INTEGER,
    category_id INTEGER,
    name TEXT NOT NULL,
    selection_type TEXT NOT NULL DEFAULT 'SINGLE',
    required INTEGER NOT NULL DEFAULT 0,
    min_select INTEGER NOT NULL DEFAULT 0,
    max_select INTEGER NOT NULL DEFAULT 1,
    sort_order INTEGER NOT NULL DEFAULT 0,
    active INTEGER NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES menu_categories(id)
);

CREATE TABLE IF NOT EXISTS modifier_options (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    group_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    price_delta REAL NOT NULL DEFAULT 0,
    is_default INTEGER NOT NULL DEFAULT 0,
    available INTEGER NOT NULL DEFAULT 1,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES modifier_groups(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transaction_item_modifiers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    transaction_item_id INTEGER NOT NULL,
    modifier_option_id INTEGER,
    group_name TEXT NOT NULL,
    option_name TEXT NOT NULL,
    price_delta REAL NOT NULL DEFAULT 0,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_item_id) REFERENCES transaction_items(id) ON DELETE CASCADE,
    FOREIGN KEY (modifier_option_id) REFERENCES modifier_options(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_modifier_groups_item ON modifier_groups (menu_item_id);
CREATE INDEX IF NOT EXISTS idx_modifier_groups_category ON modifier_groups (category_id);
CREATE INDEX IF NOT EXISTS idx_modifier_options_group ON modifier_options (group_id);
CREATE INDEX IF NOT EXISTS idx_transaction_item_modifiers_item
    ON transaction_item_modifiers (transaction_item_id);

INSERT INTO schema_migrations (version, description)
VALUES ('V8', 'menu item modifiers (sqlite)')
ON CONFLICT (version) DO NOTHING;
