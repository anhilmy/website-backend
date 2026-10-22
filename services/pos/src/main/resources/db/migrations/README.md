# Versioned database migrations

The project currently applies schema changes with plain SQL (`schema_sqlite.sql`,
`schema_postgres.sql`) plus the ordered scripts in this folder. This is a lightweight, explicit
alternative to a full migration tool; the recommended upgrade path (Flyway/Liquibase) is noted in
`../migration_readme.md`.

## Rules

- Files are named `V<version>__<description>.sql` and applied in ascending version order.
- A migration is **never edited once applied**; add a new `V<n+1>` instead.
- Keep SQLite and PostgreSQL statements in the same file using comments to separate dialects, or
  split into `V<n>__desc_sqlite.sql` / `V<n>__desc_postgres.sql` when syntax diverges.
- Record applied versions in the `schema_migrations` table.

## Applying (manual)

SQLite:
```
sqlite3 pos_dev.db < V1__baseline.sql
sqlite3 pos_dev.db < V2__menu_category_parent_and_indexes.sql
```

PostgreSQL:
```
psql -U <user> -d <db> -f V1__baseline.sql
psql -U <user> -d <db> -f V2__menu_category_parent_and_indexes.sql
```
