# CLAUDE.md

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

# EC2 배포 (docker-compose.ec2.yml 사용, prod 프로파일 자동 활성화)
docker compose -f docker-compose.ec2.yml up -d
```

## Architecture Overview

This is a **Korean payment gateway (PG) server** built with Spring Boot / Java 17, using Domain-Driven Design with Hexagonal Architecture.

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

### URL Routing

`WebMvcConfig` automatically prepends `/api` to all `@RestController` beans. `@Controller` beans (Thymeleaf views) are **not** prefixed.

| Controller type | Prefix | Example |
|----------------|--------|---------|
| `@RestController` | `/api` | `/api/payments`, `/api/merchants` |
| `@Controller` | none | `/card-form/register`, `/admin/**` |

### Cross-Module Communication

Modules communicate through **ports (interfaces)** defined in each module's `application/` layer — never through direct service injection across domains. Example: `PaymentPort` in the `receipt` module defines what it needs from payments; `PaymentAdapter` in the payment module implements it.

### Event-Driven Flow

Payment processing is event-driven using Spring's `ApplicationEventPublisher`:
1. `PaymentService.create()` saves payment (READY) and publishes `PaymentCreatedEvent`
2. `@TransactionalEventListener(AFTER_COMMIT)` triggers `PaymentOrchestratorService`
3. Orchestrator transitions payment to AUTHORIZING and publishes `AuthorizationStartedEvent`
4. `PaymentAuthorizationProcessor` calls the card company API
5. `PaymentStatusChangedEvent` triggers webhook delivery to merchant

### Card Company Port Registry

`CardCompanyPortRegistry` loads all ACTIVE card companies from DB at startup (`@PostConstruct`) and caches `CardCompanyConnect` instances in memory (keyed by `cardCompanyCode`). **After updating `card_companies.base_url` in DB, the PG server must be restarted** for the change to take effect. A `refresh()` method exists but is not exposed via API.

### Billing Key Registration Flow

Browser-redirect flow (not a REST API call from the merchant):
1. Merchant redirects user browser to `GET /card-form/register?token={billingKeyRegisterToken}`
2. `FormDataAuthFilter` verifies the token on `GET /card-form/register` and `POST /card-form/register/start`
3. PG server calls card company's `POST /api/card-registration-session` with a callback URL
4. Card company redirects user browser to its own registration page
5. After card input, card company redirects browser back to `GET /card-form/callback/register?token=...&authCode=...`
6. PG server issues billing key via card company API, then delivers webhook to merchant

**Billing key register token** is a custom 2-part token (`payloadB64.sigB64`), not JWT. Payload contains `apiKey`, `returnUrl`, `webhookUrl`, `iat`, `exp`, `nonce`, `purpose`. Signed with HMAC-SHA256 using the merchant's `apiSecret`. Nonce is consumed once in Redis to prevent replay attacks.

### Distributed Lock (`payment/infrastructure/lock`)

`PaymentProcessDistributedLock` uses Redis `SET NX EX` to prevent duplicate async execution of authorization and refund across multiple instances. Key format: `pg:lock:payment:authorize:{paymentId}` / `pg:lock:payment:refund:{paymentId}`. Release uses a Lua script to ensure only the lock owner can delete (token-safe). TTL defaults to 180s.

### Long-Running Retry Jobs (`common/retry`)

DB-backed job queue (not in-memory) for resilient async operations:
- `RetryJob` entity: `PENDING → RUNNING → SUCCEEDED/FAILED/DEAD`
- `RetryJobWorker` polls with pessimistic locking to prevent duplicate execution
- Used for: webhook delivery retries, payment/refund compensation
- Enabled via `app.retry.enabled=true` (default `false` in dev)
- Retry policies (delays, multipliers, max attempts) are configurable in `application.yml`

### Authentication

- **Merchant API** (`/api/**`): Header-based via `X-API-KEY` + `X-API-SECRET`, validated by `MerchantAuthFilter`
- **Billing key form** (`/card-form/register`, `/card-form/register/start`): Query param `token` verified by `FormDataAuthFilter`
- **Admin portal** (`/admin/**`): Form-based login with ADMIN role
- CSRF disabled for `/api/**`
- Paths excluded from `MerchantAuthFilter`: `/api/admin/`, `/actuator/`, `/api/merchant-applications` (POST), `/api/merchants/credentials` (POST)

### Persistence

- **MySQL** via Spring Data JPA — aggregate roots are JPA entities, value objects use `@Converter`
- **Redis** — card registration session tokens (TTL 900s), distributed lock tokens, billing key register nonces
- **Flyway** manages schema migrations (`V1__`, `V2__`, etc.). `baseline-on-migrate: true`, `baseline-version: 1`. Operational data (e.g. `card_companies.base_url`) is managed directly in DB, not via migrations.
- JPA `ddl-auto: validate` in both dev and prod

### Webhook Signature

All outbound webhooks (payment status change + billing key registered) include `X-PG-Signature: Base64(HMAC-SHA256(rawBodyJson, apiSecret))`. The signing key is the merchant's `apiSecret`. Body must be verified before parsing — do not re-serialize after JSON parsing.

## Key Design Decisions

- **Two-transaction payment flow**: Payment creation (Tx1) and authorization start (Tx2) are separate transactions. If Tx2 fails, a compensation step marks the payment `ABORTED`.
- **Idempotency fingerprint**: The `idempotency` module computes a SHA-256 hash of the request body so identical concurrent requests resolve to the same payment ID.
- **No cross-domain service injection**: Use ports/adapters pattern. If module A needs data from module B, define a port interface in A and implement it in B.
- **Spring Retry on card company calls**: 4 attempts with exponential backoff; `CardCompanyTransientException` vs. permanent errors determine retry eligibility. Only used for payment approve/refund, not for billing key session creation.
- **`open-in-view: false`**: Lazy loading must be handled explicitly within transaction boundaries.
- **`RestTemplate` has no timeout configured**: Hanging card company responses will block the thread indefinitely. Add connection/read timeout in `HttpClientConfig` if needed.

## EC2 Deployment Notes

- PG server: `docker-compose.ec2.yml`, `prod` profile, port 8080
- Card company (mock): separate EC2, port 8080
- `card_companies.base_url` in DB must include the port (e.g. `http://<ip>:8080`). Missing port causes silent connection to port 80.
- After any `card_companies` DB change, restart the PG container so `CardCompanyPortRegistry` re-initializes.
- Monitoring: Prometheus scrapes `/actuator/prometheus` on the PG EC2; Grafana at `docker-compose.monitoring.yml`
