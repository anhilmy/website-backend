MAKEFILE_PATH := $(lastword $(MAKEFILE_LIST))
MAKEFILE_DIR := $(dir $(realpath $(MAKEFILE_PATH)))

DB_URL ?= postgres://backenduser:8VX8QBCZD9Guy330k@localhost:5432/website_backend?sslmode=disable
MIGRATIONS_DIR = $(MAKEFILE_DIR)/../migrations
MIGRATE_CMD = migrate -path $(MIGRATIONS_DIR) -database "$(DB_URL)"
COMPOSE_FILE = docker-compose.db.yml

# Get timestamp in migrate format (UTC to match naming)
TIMESTAMP := $(shell date -u +"%Y%m%d%H%M%S")

# Create migration and rename files with module prefix
db.create:
ifndef name
	$(error name is required. Usage: make create name=add_table module=users)
endif
	migrate create -ext sql -dir $(MIGRATIONS_DIR) -seq $(module)_$(name)
	@echo "Created migration with module prefix: $(module)"

# Run migrations
db.up:
	$(MIGRATE_CMD) up

db.down:
	$(MIGRATE_CMD) down

db.redo:
	$(MIGRATE_CMD) down 1
	$(MIGRATE_CMD) up 1

db.drop:
	$(MIGRATE_CMD) drop -f

db.force:
	$(MIGRATE_CMD) force $(version)

db.start:
	@docker compose -f $(MAKEFILE_DIR)$(COMPOSE_FILE) up -d

db.stop:
	@docker compose -f $(MAKEFILE_DIR)$(COMPOSE_FILE) down

db.restart:
	@docker compose -f $(MAKEFILE_DIR)$(COMPOSE_FILE) restart

.PHONY: db.create db.up db.down db.redo db.drop db.start db.stop db.restart db.force
