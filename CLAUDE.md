so!# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build
./gradlew clean bootJar -x test

# Run locally (requires MySQL on localhost:3306/pg and Redis on localhost:6379)
./gradlew bootRun

# Run unit tests (excludes Integration tag)
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.pg.payment.application.PaymentServiceTest"

# Coverage report (HTML: build/reports/jacoco/test/html/index.html)
./gradlew test jacocoTestReport
```

## Architecture Overview

This is a **Korean payment gateway (PG) server** built with Spring Boot 4 / Java 17, using Domain-Driven Design with Hexagonal Architecture.

### Module Structure

Each domain module follows the same internal layering:
```
domain/         - Aggregate roots, value objects, domain events
application/    - Use cases, event listeners, ports (interfaces for cross-module access)
infrastructure/ - JPA repositories, HTTP clients, Redis adapters
presentation/   - Controllers, request/response DTOs
```

### Domains

- **`payment`** — Core domain. Payment status machine: `READY → AUTHORIZING → AUTHORIZED/AUTHORIZE_FAILED → CANCELLING → CANCELED/CANCEL_FAILED`. Payment creation and authorization are two separate transactions coordinated by `PaymentOrchestratorService`.
- **`merchant`** — Merchant accounts, API key/secret generation, status management.
- **`merchantapplication`** — Merchant onboarding workflow with approval/rejection.
- **`card_company`** — Card company integrations; manages billing key registration sessions via Redis (TTL 900s).
- **`receipt`** — PDF receipt generation triggered by payment authorization events.
- **`idempotency`** — Request fingerprinting (SHA-256 of request body) to prevent duplicate payments.
- **`common`** — Shared infrastructure: retry job queue, security config, error handling, Redis cache utilities.
- **`admin`** — Thymeleaf-based admin portal for merchant management.

### Cross-Module Communication

Modules communicate through **ports (interfaces)** defined in each module's `application/` layer — never through direct service injection across domains. Example: `PaymentPort` in the `receipt` module defines what it needs from payments; `PaymentAdapter` in the payment module implements it.

### Event-Driven Flow

Payment processing is event-driven using Spring's `ApplicationEventPublisher`:
1. `PaymentService.create()` saves payment (READY) and publishes `PaymentCreatedEvent`
2. `@TransactionalEventListener(AFTER_COMMIT)` triggers `PaymentOrchestratorService`
3. Orchestrator transitions payment to AUTHORIZING and publishes `AuthorizationStartedEvent`
4. `PaymentAuthorizationProcessor` calls the card company API
5. `PaymentStatusChangedEvent` triggers webhook delivery to merchant

### Long-Running Retry Jobs (`common/retry`)

DB-backed job queue (not in-memory) for resilient async operations:
- `RetryJob` entity: `PENDING → RUNNING → SUCCEEDED/FAILED/DEAD`
- `RetryJobWorker` polls with pessimistic locking to prevent duplicate execution
- Used for: webhook delivery retries, payment/refund compensation
- Retry policies (delays, multipliers, max attempts) are configurable in `application.yml`

### Authentication

- **Merchant API** (`/api/**`): Header-based via `X-API-KEY` + `X-API-SECRET`, validated by `MerchantAuthFilter`
- **Admin portal** (`/admin/**`): Form-based login with ADMIN role
- CSRF disabled for `/api/**`

### Persistence

- **MySQL** via Spring Data JPA — aggregate roots are JPA entities, value objects use `@Converter`
- **Redis** — card registration session tokens, caching
- JPA `ddl-auto: update` in dev, `validate` in prod

## Key Design Decisions

- **Two-transaction payment flow**: Payment creation (Tx1) and authorization start (Tx2) are separate transactions. If Tx2 fails, a compensation step marks the payment `ABORTED`.
- **Idempotency fingerprint**: The `idempotency` module computes a SHA-256 hash of the request body so identical concurrent requests resolve to the same payment ID.
- **No cross-domain service injection**: Use ports/adapters pattern. If module A needs data from module B, define a port interface in A and implement it in B.
- **Spring Retry on card company calls**: 4 attempts with exponential backoff; `CardCompanyTransientException` vs. permanent errors determine retry eligibility.
- **`open-in-view: false`**: Lazy loading must be handled explicitly within transaction boundaries.
