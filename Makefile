.PHONY: help up down build test test-unit test-integration test-api coverage-view clean install run-local k6-load

help:
	@echo "Bloomberg FX API - Available Commands"
	@echo "====================================="
	@echo "make install          - Install Maven dependencies"
	@echo "make build            - Build the application"
	@echo "make test             - Run all tests with coverage"
	@echo "make test-unit        - Run unit tests only"
	@echo "make test-integration - Run integration tests only"
	@echo "make test-api         - Run REST Assured API tests only"
	@echo "make coverage-view    - Open all coverage reports in browser"
	@echo "make up               - Start Docker containers"
	@echo "make down             - Stop Docker containers"
	@echo "make clean            - Clean build artifacts"
	@echo "make run-local        - Run application locally"
	@echo "make db-up            - Start PostgreSQL only"
	@echo "make db-down          - Stop PostgreSQL"
	@echo "make logs             - View application logs"
	@echo "make k6-load          - Run K6 performance tests"

install:
	@echo "Installing dependencies..."
	mvn clean install -DskipTests

build:
	@echo "Building application..."
	mvn clean package -DskipTests

test-unit:
	@echo "Running unit tests only..."
	mvn test -Dtest="com.bloomberg.fxdeals.unit.*Test"
	@echo "Unit test report: target/site/jacoco/index.html"

test-integration:
	@echo "Running integration tests only..."
	mvn verify -DskipUTs -Dit.test="com.bloomberg.fxdeals.integration.*IT,com.bloomberg.fxdeals.integration.*IntegrationTest"
	@echo "Integration test report: target/site/jacoco-it/index.html"

test-api:
	@echo "Running REST Assured API tests only..."
	mvn test -Dtest="com.bloomberg.fxdeals.api.*Test"

test:
	@echo "Running all tests with coverage reports..."
	mvn clean verify
	@echo "========================================"
	@echo "Coverage Reports Generated:"
	@echo "  - Unit Tests:        target/site/jacoco/index.html"
	@echo "  - Integration Tests: target/site/jacoco-it/index.html"
	@echo "  - Merged Report:     target/reporting/jacoco-merged/index.html"
	@echo "========================================"

coverage-view:
	@echo "Opening coverage reports..."
	@xdg-open target/site/jacoco/index.html 2>/dev/null || open target/site/jacoco/index.html 2>/dev/null || echo "Unit test coverage: target/site/jacoco/index.html"
	@sleep 1
	@xdg-open target/site/jacoco-it/index.html 2>/dev/null || open target/site/jacoco-it/index.html 2>/dev/null || echo "Integration test coverage: target/site/jacoco-it/index.html"
	@sleep 1
	@xdg-open target/reporting/jacoco-merged/index.html 2>/dev/null || open target/reporting/jacoco-merged/index.html 2>/dev/null || echo "Merged coverage: target/reporting/jacoco-merged/index.html"

up:
	@echo "Starting Docker containers..."
	docker compose up --build -d
	@echo "Application running at http://localhost:8080"

down:
	@echo "Stopping Docker containers..."
	docker compose down

db-up:
	@echo "Starting PostgreSQL..."
	docker compose up postgres -d
	@echo "PostgreSQL running at localhost:5432"

db-down:
	@echo "Stopping PostgreSQL..."
	docker compose stop postgres

run-local: db-up
	@echo "Running application locally..."
	mvn spring-boot:run

logs:
	docker compose logs -f app

k6-load:
	@echo "Running K6 load test (single + batch scenarios)..."
	k6 run k6/load-test.js

clean:
	@echo "Cleaning build artifacts..."
	mvn clean
	docker compose down -v