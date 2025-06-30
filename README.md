# BookStore Microservices System

A Spring Boot + Spring Cloud microservices e-commerce backend: user auth (JWT),
a book catalog, and order placement with inventory deduction across services.

## Architecture

```
                        ┌─────────────────┐
                        │  Eureka Server   │  (service registry, :8761)
                        └────────▲─────────┘
                                 │ registers
        ┌────────────────────────┼────────────────────────┐
        │                        │                         │
┌───────┴───────┐      ┌─────────┴────────┐      ┌─────────┴────────┐
│  auth-service  │      │   book-service   │      │   order-service  │
│    :8081       │      │      :8082       │      │      :8083       │
│  (users, JWT)  │      │ (catalog, stock) │◄─────┤ (Feign client to │
└────────────────┘      └──────────────────┘      │  book-service)   │
        ▲                        ▲                  └──────────────────┘
        │                        │                         ▲
        └────────────┬───────────┴─────────────────────────┘
                      │
              ┌───────┴────────┐
              │  api-gateway    │  (:8080 — single entry point, JWT check)
              └───────┬────────┘
                      │
                   Client
```

- **eureka-server** — service discovery. Every other service registers itself here.
- **api-gateway** — single entry point (port 8080). Routes `/api/auth/**`,
  `/api/books/**`, `/api/orders/**` to the right service by looking them up in
  Eureka. Also runs a global JWT filter that blocks unauthenticated requests to
  protected routes before they're forwarded.
- **auth-service** — registration/login, issues JWTs (HS256, `jjwt`), stores
  users (BCrypt-hashed passwords) in an H2 in-memory DB.
- **book-service** — book catalog CRUD. Reads are public; writes require
  `ROLE_ADMIN`. Stock is adjusted via `PATCH /api/books/{id}/stock` and uses
  JPA optimistic locking (`@Version`) so concurrent orders can't oversell.
- **order-service** — places orders. Calls book-service via **OpenFeign**
  (resolved through Eureka) to check price/availability and deduct stock. If
  stock deduction fails partway through a multi-item order, already-deducted
  items are restocked (best-effort compensation — see Limitations).

Each service validates the JWT itself too (not just the gateway) — defense in
depth, and it means services still work correctly if called directly in dev/testing.

## Tech Stack

- Java 17, Spring Boot 3.2.5, Spring Cloud 2023.0.1 (Leyton)
- Spring Cloud Netflix Eureka (discovery), Spring Cloud Gateway, OpenFeign
- Spring Security (stateless, JWT-based, method-level `@PreAuthorize`)
- Spring Data JPA + H2 (in-memory, swap for MySQL/Postgres in production — see below)
- jjwt 0.12.5 for JWT signing/parsing
- Lombok

## Running Locally

### Option 1: Run with Maven (manual startup)

Start in this order (each in its own terminal), from each service's folder:

```bash
# 1. Eureka first — everything else needs it to register
cd eureka-server && ./mvnw spring-boot:run

# 2. Then the rest (order doesn't matter much between these three)
cd auth-service && ./mvnw spring-boot:run
cd book-service && ./mvnw spring-boot:run
cd order-service && ./mvnw spring-boot:run

# 3. Gateway last (or anytime — it'll just retry registering routes)
cd api-gateway && ./mvnw spring-boot:run
```

Check registration: open `http://localhost:8761` — you should see AUTH-SERVICE,
BOOK-SERVICE, ORDER-SERVICE, API-GATEWAY listed as UP within ~30s of each starting.

No Maven wrapper JAR? Run `mvn spring-boot:run` instead if you have Maven installed,
or `mvn -N wrapper:wrapper` inside each service folder to generate `mvnw`.

### Option 2: Run with Docker Compose

This repository includes a compose file to start the full stack in one command.
Make sure Docker Desktop is installed and running, then from the project root:

```bash
docker compose up --build -d
```

Useful checks:

```bash
docker compose ps
docker compose logs -f
docker compose down
```

The compose stack exposes:

- Eureka: `http://localhost:8761`
- Gateway: `http://localhost:8080`
- Auth Service: `http://localhost:8081`
- Book Service: `http://localhost:8082`
- Order Service: `http://localhost:8083`

Sample environment variables are in `.env.example`. Copy it to `.env` and adjust
secrets if needed for your local environment.

## Trying It Out

All requests go through the gateway on port **8080**.

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Sahil","email":"sahil@example.com","password":"password123"}'
# -> returns { token, userId, name, email, role }

# Save the token
TOKEN="<paste token here>"

# Browse books (public, no token needed)
curl http://localhost:8080/api/books

# Search
curl "http://localhost:8080/api/books/search?query=clean"

# Place an order (needs token)
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"bookId":1,"quantity":2},{"bookId":3,"quantity":1}]}'

# My orders
curl http://localhost:8080/api/orders/my -H "Authorization: Bearer $TOKEN"

# Cancel an order (restocks the books)
curl -X POST http://localhost:8080/api/orders/1/cancel -H "Authorization: Bearer $TOKEN"
```

To test admin-only endpoints (create/update/delete books), you'd need a user
with `ROLE_ADMIN`. The demo seeds every registration as `CUSTOMER` — easiest
way to test is to insert a row directly via the H2 console
(`http://localhost:8081/h2-console`, JDBC URL `jdbc:h2:mem:authdb`) and set
`role = 'ADMIN'`, or add a temporary seed admin in `auth-service`'s `data.sql`.

## What's Real vs. What's Simplified (be honest about this in interviews)

**Real / production-shaped:**
- Actual service discovery and client-side load balancing (Eureka + Feign),
  not hardcoded URLs.
- Stateless JWT auth validated independently by each service.
- Optimistic locking on stock (`@Version`) to prevent lost updates under
  concurrent orders for the *same* book.
- Layered structure (controller → service → repository) with DTOs, not
  entities leaking out of controllers.

**Simplified for a demo project — say this upfront if asked:**
- **H2 in-memory DB per service.** Data resets on restart. Production would
  use a real Postgres/MySQL instance per service (database-per-service
  pattern), with Flyway/Liquibase migrations instead of `ddl-auto: update`.
- **No distributed transaction / saga for order placement.** The
  "deduct stock, and restock on failure" logic in `OrderService.placeOrder`
  is *compensation*, not a real saga — if the restock call itself fails
  (network blip, book-service down), inventory can drift and only a log
  line says so. A production system would use an outbox pattern + message
  broker (Kafka/RabbitMQ) so this is durable and retryable.
- **No circuit breaker.** If book-service is down, order-service's Feign
  calls just fail/timeout — there's no Resilience4j fallback. Worth adding
  as a follow-up if you want to talk about resilience patterns.
- **Single shared JWT secret via `application.yml`.** Fine for a demo;
  production would pull this from a secrets manager (Vault, AWS Secrets
  Manager) and likely use RS256 with a public/private keypair so services
  only need the public key to verify.
- **No rate limiting, no API versioning, no centralized config server**
  (Spring Cloud Config) — each service has its own `application.yml`.
- **The `/stock` endpoint on book-service isn't locked to internal traffic.**
  In production you'd restrict it (network policy, mTLS, or a
  service-to-service token) so only order-service can call it.

## Suggested Next Extensions (if you want to keep building)

1. Add Resilience4j circuit breaker + fallback around the Feign calls.
2. Swap H2 for Postgres with Flyway migrations, run via docker-compose.
3. Add a Config Server so all five `application.yml`s live in one Git repo.
4. Add an outbox table + Kafka to make order placement actually durable.
5. Keep the Docker Compose setup and add a real production-ready `.env`/secrets strategy.
6. Add integration tests with Testcontainers.

## Project Structure

```
bookstore-microservices/
├── eureka-server/     # service registry
├── api-gateway/        # single entry point, JWT check, routing
├── auth-service/        # users, register/login, JWT issuing
├── book-service/         # catalog CRUD, stock management
└── order-service/         # order placement, Feign client to book-service
```

## Project Timeline

- May 2025: initial microservices platform created
- June 2025: services, security, orchestration, and documentation completed
