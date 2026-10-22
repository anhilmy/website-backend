# Point of Sales System Makefile
# For Windows PowerShell environment

# Variables
PROJECT_NAME := pos-system
MAIN_CLASS := com.anhilmy.pos.App
MAVEN_OPTS := -Dmaven.compiler.source=21 -Dmaven.compiler.target=21
JAR_NAME := spark-app-1.0-SNAPSHOT.jar
TARGET_DIR := target

# Default target
.PHONY: help
help:
	@echo "Available targets:"
	@echo "  build     - Compile and package the project"
	@echo "  run       - Run the application"
	@echo "  test      - Run tests"
	@echo "  clean     - Clean build artifacts"
	@echo "  install   - Install dependencies"
	@echo "  package   - Package the application"
	@echo "  dev       - Run in development mode (with auto-reload)"
	@echo "  check     - Check for compilation errors"
	@echo "  help      - Show this help message"

# Build the project
.PHONY: build
build: clean install package
	@echo "Build completed successfully!"

# Install dependencies
.PHONY: install
install:
	@echo "Installing dependencies..."
	mvn dependency:resolve

# Compile the project
.PHONY: compile
compile:
	@echo "Compiling project..."
	mvn compile

# Package the project
.PHONY: package
package:
	@echo "Packaging project..."
	mvn package -DskipTests

# Run the application
.PHONY: run
run: compile
	@echo "Starting POS System..."
	mvn exec:java -Dexec.mainClass="$(MAIN_CLASS)" -Dexec.cleanupDaemonThreads=false

# Run with Maven exec plugin (alternative)
.PHONY: run-jar
run-jar: package
	@echo "Running from JAR..."
	java -cp "$(TARGET_DIR)/$(JAR_NAME);$(TARGET_DIR)/lib/*" $(MAIN_CLASS)

# Run in development mode
.PHONY: dev
dev:
	@echo "Starting in development mode..."
	mvn compile exec:java -Dexec.mainClass="$(MAIN_CLASS)" -Dexec.args="dev"

# Run tests
.PHONY: test
test:
	@echo "Running tests..."
	mvn test

# Clean build artifacts
.PHONY: clean
clean:
	@echo "Cleaning build artifacts..."
	mvn clean

# Check for compilation errors without running tests
.PHONY: check
check:
	@echo "Checking for compilation errors..."
	mvn compile -DskipTests

# Generate project reports
.PHONY: reports
reports:
	@echo "Generating project reports..."
	mvn site

# Display project info
.PHONY: info
info:
	@echo "Project Information:"
	@echo "  Name: $(PROJECT_NAME)"
	@echo "  Main Class: $(MAIN_CLASS)"
	@echo "  JAR: $(JAR_NAME)"
	@echo "  Target Directory: $(TARGET_DIR)"
	mvn --version
	java --version

# Copy dependencies to lib folder
.PHONY: copy-deps
copy-deps:
	@echo "Copying dependencies..."
	mvn dependency:copy-dependencies -DoutputDirectory=$(TARGET_DIR)/lib

# Full build with dependencies
.PHONY: build-full
build-full: clean install copy-deps package
	@echo "Full build completed with all dependencies!"

# Quick build (no clean)
.PHONY: quick-build
quick-build: compile package
	@echo "Quick build completed!"

# Verify project setup
.PHONY: verify
verify:
	@echo "Verifying project setup..."
	mvn validate
	mvn dependency:analyze

# Create executable JAR with dependencies
.PHONY: uber-jar
uber-jar:
	@echo "Creating uber JAR with all dependencies..."
	mvn clean compile assembly:single

# Format code (if spotless plugin is available)
.PHONY: format
format:
	@echo "Formatting code..."
	-mvn spotless:apply 2>NUL || echo "Spotless plugin not configured, skipping formatting"

# Security check
.PHONY: security-check
security-check:
	@echo "Running security checks..."
	mvn dependency:check-vulnerable-deps 2>NUL || mvn versions:display-dependency-updates

# Show dependency tree
.PHONY: deps
deps:
	@echo "Showing dependency tree..."
	mvn dependency:tree

# Profile-based runs
.PHONY: run-dev
run-dev:
	@echo "Running with development profile..."
	mvn compile exec:java -Dexec.mainClass="$(MAIN_CLASS)" -Dspring.profiles.active=dev

.PHONY: run-prod
run-prod:
	@echo "Running with production profile..."
	mvn compile exec:java -Dexec.mainClass="$(MAIN_CLASS)" -Dspring.profiles.active=prod

# Debug mode
.PHONY: debug
debug:
	@echo "Starting in debug mode (port 5005)..."
	mvn compile exec:java -Dexec.mainClass="$(MAIN_CLASS)" -Dexec.args="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"

# Monitor file changes and restart (requires additional tools)
.PHONY: watch
watch:
	@echo "Watching for file changes..."
	@echo "Note: This requires external file watching tools"
	@echo "Consider using 'mvn spring-boot:run' if using Spring Boot"
	mvn compile exec:java -Dexec.mainClass="$(MAIN_CLASS)"

# Database operations (if applicable)
.PHONY: db-migrate
db-migrate:
	@echo "Running database migrations..."
	@echo "Implement database migration commands here"

# Docker operations
.PHONY: docker-build
docker-build:
	@echo "Building Docker image..."
	docker build -t $(PROJECT_NAME) .

.PHONY: docker-run
docker-run:
	@echo "Running Docker container..."
	docker run -p 4567:4567 $(PROJECT_NAME)

# Docker Compose operations
.PHONY: docker-up
docker-up:
	@echo "Starting all services with Docker Compose..."
	docker-compose --env-file .env.docker up -d

.PHONY: docker-down
docker-down:
	@echo "Stopping all services..."
	docker-compose down

.PHONY: docker-logs
docker-logs:
	@echo "Showing Docker Compose logs..."
	docker-compose logs -f

.PHONY: docker-build-app
docker-build-app:
	@echo "Building only the application container..."
	docker-compose build pos-app

.PHONY: docker-restart-app
docker-restart-app:
	@echo "Restarting application container..."
	docker-compose restart pos-app

.PHONY: docker-db-shell
docker-db-shell:
	@echo "Connecting to PostgreSQL database..."
	docker-compose exec postgres psql -U backenduser -d website_backend

.PHONY: docker-app-shell
docker-app-shell:
	@echo "Connecting to application container shell..."
	docker-compose exec pos-app sh

.PHONY: docker-clean
docker-clean:
	@echo "Cleaning Docker resources..."
	docker-compose down -v
	docker system prune -f

.PHONY: docker-reset
docker-reset: docker-clean
	@echo "Resetting all Docker containers and volumes..."
	docker-compose down -v --remove-orphans
	docker-compose pull
	docker-compose up -d

.PHONY: docker-backup-db
docker-backup-db:
	@echo "Creating database backup..."
	docker-compose exec postgres pg_dump -U backenduser website_backend > backup_$(shell date +%Y%m%d_%H%M%S).sql

.PHONY: docker-status
docker-status:
	@echo "Docker Compose service status:"
	docker-compose ps

.PHONY: docker-dev
docker-dev:
	@echo "Starting development environment..."
	docker-compose --env-file .env.docker -f docker-compose.yml up --build

# Show running processes on the application port
.PHONY: ps
ps:
	@echo "Checking processes on port 4567..."
	netstat -an | findstr :4567 || echo "No processes found on port 4567"

# Kill process on application port
.PHONY: kill-port
kill-port:
	@echo "Killing processes on port 4567..."
	@for /f "tokens=5" %a in ('netstat -aon ^| findstr :4567') do taskkill /f /pid %a 2>NUL || echo "No processes to kill"

# Show application logs (if log file exists)
.PHONY: logs
logs:
	@echo "Showing application logs..."
	@if exist logs\app.log (type logs\app.log | more) else echo "No log file found"

# Complete workflow: clean, build, test, run
.PHONY: all
all: clean build test
	@echo "Complete build and test cycle finished!"
	@echo "Run 'make run' to start the application"