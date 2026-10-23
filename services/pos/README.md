# POS Service

Backend for the Point of Sale system: cashier authentication, menu management and order
lifecycle, built with **Java 21 + SparkJava + Hibernate 6**.

## Stack

| Concern | Choice |
|---|---|
| Language / runtime | Java 21 |
| HTTP | SparkJava 2.9 |
| Persistence | Hibernate ORM 6.5 (JPA annotations) |
| Databases | SQLite (dev/test) and PostgreSQL (docker/prod), switchable by config |
| Pooling | HikariCP |
| Auth | JWT (HS256, jjwt) + BCrypt password hashing |
| JSON | Gson (with ISO-8601 date adapters and safe field exclusions) |
| Logging | SLF4J + Logback (console + rotating file), optional Kafka streaming |
| Validation | Jakarta Bean Validation + hand-rolled `InputValidator` |
| Tests | JUnit 5 + Mockito |

## Architecture

```
HTTP request
  ├─ CORS / SecurityHeaders / RequestResponseLogger / RateLimit   (before filters)
  ├─ DbManager.openSession()                                       (one Hibernate session + tx per request)
  ├─ AuthenticationFilter                                          (JWT → request attributes)
  ├─ Controller  →  Service  →  Repository  →  Hibernate Session
  ├─ Routes.wrap()  commits on success / rolls back on failure
  └─ exception handlers map ApiException → HTTP status + JSON envelope
```

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the deeper walkthrough.

### Packages (`src/main/java/com/anhilmy/pos`)

| Package | Responsibility |
|---|---|
| `config` | Environment/profile-aware configuration + DB dialect selection |
| `db` | `DbManager` (thread-local session), `DataSeeder` |
| `model` | JPA entities: `User`, `MenuCategory`, `MenuItem`, `Transaction`, `TransactionItem` |
| `repository` | `BaseRepository` + concrete repositories |
| `service` | Business logic and rules |
| `controller` | SparkJava route registration |
| `middleware` | Auth, CORS, security headers, rate limiting, request logging |
| `dto` | Request payloads + `ApiResponse`/`PageResponse` envelopes |
| `exception` | `ApiException` hierarchy mapped to HTTP statuses |
| `logging` | `LoggingManager`, `KafkaLogger` |
| `util` | JWT, password hashing, JSON, validation, responder, route wrapper |

## Configuration

Resolution order: **environment variable → `application-<PROFILE>.properties` → `application.properties`**.
`PROFILE` defaults to `dev`. See [`.env.example`](.env.example).

Key settings: `db.type`, `db.url`, `db.hbm2ddl`, `server.port`, `jwt.secret`, `jwt.expiration`,
`order.tax-rate`, `order.max-items`, `log.payload.enabled`, `kafka.enabled`, `seed.enabled`.

## Running

### Local (SQLite)

```bash
# build the runnable fat JAR: target/pos-app.jar
mvn package -DskipTests

# run (PROFILE=dev, SQLite pos_dev.db, schema auto-created via db.hbm2ddl=update)
java -jar target/pos-app.jar
# or: make run
```

On first start with `seed.enabled=true` (and a non-`prod` profile) two accounts and a sample menu
are created:

| Username | Password | Role |
|---|---|---|
| `manager` | `manager123` | MANAGER |
| `cashier` | `cashier123` | CASHIER |

### Docker Compose (PostgreSQL)

A real JWT secret is **required** — none is shipped:

```bash
export JWT_SECRET=$(openssl rand -hex 32)
docker compose --env-file .env.docker up --build
```

Runs Postgres (schema + sample data auto-applied from `src/main/resources/db/`), the app on
`:4567`, plus optional Redis/Kafka/pgAdmin/nginx services. The `docker`/`prod` profiles run with
`db.hbm2ddl=validate`, so the SQL schema must match the entities exactly.

### Tests

```bash
mvn test
```

Tests run against a throwaway SQLite database (`target/pos_test.db`).

## API

Base path `/api/v1`. All responses use the envelope
`{ success, data, message, error, timestamp }`. Authenticate via `POST /api/v1/auth/login` and
send `Authorization: Bearer <token>`. Interactive docs at **`/docs`** (spec: `/openapi.yaml`).

| Group | Method & path | Auth |
|---|---|---|
| Health | `GET /health`, `GET /api/v1/healthcheck`, `GET /api/v1/healthcheck/ready` | public |
| Auth | `POST /auth/login`, `POST /auth/logout`, `GET /auth/me` | public / bearer |
| Public menu | `GET /menu/categories`, `GET /menu/items`, `GET /menu/items/{id}` | public |
| Categories | `GET/POST /categories`, `GET/PUT/DELETE /categories/{id}` | bearer |
| Items | `GET/POST /items`, `GET/PUT/DELETE /items/{id}` | bearer |
| Orders | `GET/POST /orders`, `GET /orders/{id}`, `POST /orders/{id}/items`, `PUT/DELETE /orders/{id}/items/{itemId}`, `POST /orders/{id}/complete`, `POST /orders/{id}/cancel` | bearer |
| Tables | `GET/POST /tables`, `GET/PUT/DELETE /tables/{id}`, `PATCH /tables/{id}/status`, `POST /tables/{id}/open`, `/assign`, `/release`, `/join`, `/unjoin` | bearer (layout changes: MANAGER) |
| Users | `GET/POST /users`, `GET /users/{id}`, `PUT /users/{id}/password` | bearer, MANAGER (password: self) |

### Authorization

- Menu/category **mutations** (`POST`/`PUT`/`DELETE`) require the `MANAGER` role.
- Order routes are scoped: a cashier may only read/modify **their own** orders; managers may act
  on any order.
- `/users` management endpoints require `MANAGER`.

### Security notes

- `jwt.secret` must be a unique random value of ≥32 chars; the app **refuses to start** with the
  placeholder/weak default. Docker/prod require `JWT_SECRET` to be provided.
- Tokens are HMAC-signed (algorithm chosen by jjwt from the key length — HS384 with the shipped
  defaults; docs elsewhere may say "HS256" loosely). JWT logout uses an in-memory revocation list,
  so it does not span multiple replicas.
- Response headers include CORS, `X-Content-Type-Options`, `X-Frame-Options` and a strict CSP
  (relaxed only for the Swagger UI page, which loads its assets from a CDN).
- Demo accounts are only seeded when `seed.demo-users=true` (dev only), never in docker/prod.

### Order lifecycle

`DRAFT → (add/modify items) → COMPLETED` (immutable) or `CANCELLED`. Totals are always recomputed
from line items (`subtotal + tax`), so the stored header can never drift from the items.

### Table lifecycle

`AVAILABLE → RESERVED/OPENED(OCCUPIED) → CLEANING → AVAILABLE`, plus `JOINED` and
`OUT_OF_SERVICE`. Opening a table creates and attaches a draft order; **joining** merges a table
into a host table so both share one bill (`POST /tables/{id}/join`); releasing a table cancels any
still-open bill, frees its joined tables and marks it for cleaning. Settling or cancelling a bill
also frees its table automatically, so the floor never keeps a finished bill on a table.

## Status

| Area | State |
|---|---|
| Auth (JWT, BCrypt, logout/blacklist, role checks) | done |
| Menu categories + items (CRUD, search/filter, soft delete) | done |
| Orders (draft, items, complete, cancel, totals, immutability) | done |
| Tables (floor plan, occupancy, open/assign/release, join/unjoin) | done |
| Dual DB (SQLite/PostgreSQL), pooling, migrations | done |
| Error handling, validation, response envelope, pagination | done |
| Logging (console/file/Kafka, correlation IDs, audit) | done |
| OpenAPI spec + Swagger UI | done |
| Tests (unit + integration) | done (46 tests; no HTTP-controller/coverage gate) |
| CI (build + test + package) | done — `website-backend/.github/workflows/pos-ci.yml` |
| Redis caching, email, inventory, loyalty, reporting, metrics | not started (optional phase 13) |
