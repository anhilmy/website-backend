-- V8 (PostgreSQL): item modifier groups/options and snapshotted line modifiers.
-- Apply with: psql -U <user> -d <db> -f V8__menu_item_modifiers_postgres.sql

ALTER TABLE transaction_items ADD COLUMN IF NOT EXISTS note VARCHAR(255);

CREATE TABLE IF NOT EXISTS modifier_groups (
    id BIGSERIAL PRIMARY KEY,
    menu_item_id BIGINT,
    category_id BIGINT,
    name VARCHAR(64) NOT NULL,
    selection_type VARCHAR(16) NOT NULL DEFAULT 'SINGLE',
    required BOOLEAN NOT NULL DEFAULT FALSE,
    min_select INTEGER NOT NULL DEFAULT 0,
    max_select INTEGER NOT NULL DEFAULT 1,
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES menu_categories(id)
);

CREATE TABLE IF NOT EXISTS modifier_options (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    price_delta NUMERIC(38,2) NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES modifier_groups(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transaction_item_modifiers (
    id BIGSERIAL PRIMARY KEY,
    transaction_item_id BIGINT NOT NULL,
    modifier_option_id BIGINT,
    group_name VARCHAR(64) NOT NULL,
    option_name VARCHAR(64) NOT NULL,
    price_delta NUMERIC(38,2) NOT NULL DEFAULT 0,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_item_id) REFERENCES transaction_items(id) ON DELETE CASCADE,
    FOREIGN KEY (modifier_option_id) REFERENCES modifier_options(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_modifier_groups_item ON modifier_groups (menu_item_id);
CREATE INDEX IF NOT EXISTS idx_modifier_groups_category ON modifier_groups (category_id);
CREATE INDEX IF NOT EXISTS idx_modifier_options_group ON modifier_options (group_id);
CREATE INDEX IF NOT EXISTS idx_transaction_item_modifiers_item
    ON transaction_item_modifiers (transaction_item_id);

INSERT INTO schema_migrations (version, description)
VALUES ('V8', 'menu item modifiers (postgres)')
ON CONFLICT (version) DO NOTHING;
