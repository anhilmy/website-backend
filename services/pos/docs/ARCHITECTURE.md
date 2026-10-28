# Architecture

## Request lifecycle

```
client
  │
  ▼
SparkJava (port 4567)
  │  before filters (in order):
  │    1. CorsFilter            – CORS headers + OPTIONS preflight
  │    2. SecurityHeadersFilter – X-Content-Type-Options, CSP, ...
  │    3. RequestResponseLogger – correlation id + start time
  │    4. RateLimitFilter       – fixed-window per IP (global, stricter for /auth/login)
  │    5. DbManager.openSession() – thread-local Hibernate session + transaction
  │    6. AuthenticationFilter  – JWT → request attributes (skips public routes)
  ▼
Controller  ──►  Service  ──►  Repository  ──►  Hibernate Session
  │
  │  Routes.wrap(): commit on success, rollback on any exception
  ▼
after filter: response logging + safety-net close
  │
  ▼
exception handlers: ApiException → status/code, anything else → 500
```

## Transaction management

- **One session + transaction per request**, bound to the request thread by `DbManager`.
- Repositories never open or commit transactions; they borrow `DbManager.getSession()`.
- `Routes.wrap()` (see `util/Routes.java`) makes commit/rollback **explicit and deterministic**
  rather than depending on Spark's after-filter ordering relative to exception handlers.
- An `after` filter acts as a safety net so non-wrapped routes (e.g. readiness probe) can never
  leak a session.

## Data model

```
User 1───* Transaction 1───* TransactionItem *───1 MenuItem *───1 MenuCategory
                  │                │ 1───* TransactionItemModifier
                  │                │              │ *───0..1 ModifierOption
                  │                │                              *───1 ModifierGroup
                  │                │                                          *───1 MenuItem
                  ├───* Charge
                  ├───* Refund
                  ▲
                  │ 0..1
              DiningTable *───1 DiningTable (joinedTo host)
```

`TransactionItemModifier` copies the group/option names and price delta at order time, so menu
edits never rewrite historic bills. A line's identity is the menu item plus its sorted option ids
plus its note.

Totals are derived, never trusted from the client: `TransactionService.recalcTotals()` recomputes
`subtotal → ordered charges → total` from the line items on every mutation. `tax_amount` is the sum
of charges flagged as tax; each charge is rounded once at the end. Negative charges are discounts,
with a hard floor of zero for the total.

All monetary attributes (`MenuItem.price`, line prices, bill totals, refunds, option deltas,
computed charges) are stored as scaled `BIGINT` minor units via `MoneyConverter`, and charge
template/order values use `RateConverter` at scale 6. This removes SQLite's `REAL` drift and keeps
the same exact representation in PostgreSQL.

A `DiningTable` may point at one open `Transaction` (`currentTransaction`) and, when merged, at a
host table (`joinedTo`). Joined tables share the host's bill; releasing the host clears it for all.

## Security

- **Authentication**: HMAC-signed JWT carrying `uid`, `username`, `role` (jjwt selects the HMAC
  variant from the key length — HS384 with the default secret). Logout revokes the token via an
  in-memory blacklist (pruned on access).
- **Authorization**: `AuthenticationFilter.requireRole(...)` guards manager-only endpoints.
- **Passwords**: BCrypt (cost 12). Hashes are never serialized (excluded in `util/Json`).
- **Input**: `InputValidator` + Jakarta Validation; free text is sanitized; JSON parsing failures
  become 400s.
- **Abuse**: per-IP rate limiting; security headers on every response.

## Configuration & profiles

`ConfigManager` resolves each key from (1) environment variable, (2) `application-<PROFILE>.properties`,
(3) `application.properties`, and interpolates `${...}` placeholders. `db.type` selects the dialect
(`org.hibernate.community.dialect.SQLiteDialect` vs `PostgreSQLDialect`) and driver.

`db.hbm2ddl` is `update` in dev (convenient) and `validate` in docker/prod (schema must be applied
via `src/main/resources/db/`), so production never silently mutates the schema.

## Persistence gotcha

`util/Json` excludes a small set of fields to avoid (a) leaking `passwordHash` and (b) infinite
recursion on bidirectional associations (`MenuCategory.menuItems`/`children`,
`TransactionItem.transaction`, `TransactionItemModifier.transactionItem`, `ModifierOption.group`,
`DiningTable.joinedTo`). Add new back-references to that exclusion set when introducing them.
