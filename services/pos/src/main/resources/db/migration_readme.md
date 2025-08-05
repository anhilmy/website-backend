# Database Migration System for POS

## How to Run Migrations

### SQLite

```
cat src/main/resources/db/schema_sqlite.sql | sqlite3 pos_dev.db
```

### PostgreSQL

```
psql -U <username> -d <database> -f src/main/resources/db/schema_postgres.sql
```

## Migration Tool Recommendation

- Use [Flyway](https://flywaydb.org/) or [Liquibase](https://www.liquibase.org/) for production migrations.
- Place migration scripts in `src/main/resources/db/migrations/`.
- Configure migration tool in `pom.xml` and `application.properties`.

## Example Flyway Configuration (pom.xml)

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>9.22.3</version>
</dependency>
```

## Example Flyway Properties (application.properties)

```
flyway.enabled=true
flyway.locations=classpath:db/migrations
flyway.url=${db.url}
flyway.user=${db.username}
flyway.password=${db.password}
```
