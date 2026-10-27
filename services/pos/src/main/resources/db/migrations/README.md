# Versioned database migrations

The project applies schema changes with plain SQL (`schema_sqlite.sql`, `schema_postgres.sql`)
plus the ordered scripts in this folder. This is a lightweight, explicit alternative to a full
migration tool; the recommended upgrade path (Flyway/Liquibase) is noted in
`../migration_readme.md`.

## Rules

- Files are named `V<version>__<description>.sql` and applied in ascending version order.
- **Dialect-specific DDL lives in its own file**, suffixed `_sqlite` or `_postgres`
  (e.g. `V3__dining_tables_sqlite.sql`). A single file must never mix dialects, or the
  statements the other engine cannot parse will silently fail.
- A migration is **never edited once applied**; add a new `V<n+1>` instead.
- Record applied versions in the `schema_migrations` table (each script inserts its own row).

## Version history

| Version | Change |
|---|---|
| V1 | baseline bookkeeping (`schema_migrations`) |
| V2 | menu-category parent + lookup indexes |
| V3 | `dining_tables` (floor plan: name, area, status, current bill, join host, version) |
| V4 | `payment_methods` + `transactions.payment_reference` |
| V5 | `charge_templates`, `charges`, persisted `subtotal_amount`/`charges_amount` |
| V6 | line voids, `refunds`, `refunded_amount`, amendment links |
| V7 | optimistic `version` on bills + unique `amended_from_id` |

## Applying (manual)

SQLite:
```
sqlite3 pos_dev.db < V1__baseline.sql
sqlite3 pos_dev.db < V2__menu_category_parent_and_indexes.sql
sqlite3 pos_dev.db < V3__dining_tables_sqlite.sql
```

PostgreSQL:
```
psql -U <user> -d <db> -f V1__baseline.sql
psql -U <user> -d <db> -f V2__menu_category_parent_and_indexes.sql
psql -U <user> -d <db> -f V3__dining_tables_postgres.sql
```

Alternatively apply the full baseline directly:
`sqlite3 pos_dev.db < ../schema_sqlite.sql` / `psql -f ../schema_postgres.sql`.
