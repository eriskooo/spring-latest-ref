# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
mvn clean package          # Build JAR
mvn spring-boot:run        # Run locally (http://localhost:8080)
mvn clean package -DskipTests  # Build without tests
```

Docker:

```bash
docker build -t lorma/spring-latest-ref:snapshot .
docker run --rm -p 8080:8080 lorman/spring-latest-ref:snapshot
```

## Tests

```bash
mvn test                                         # All tests
mvn test -Dtest=AutomobilServiceImplTest         # Single class
mvn test -Dtest=AutomobilServiceImplTest#findById_najde_auto_ked_existuje  # Single method
```

Test types:

- **Unit**: `service/AutomobilServiceImplTest` — Mockito + `StepVerifier`, no Spring context
- **Circuit breaker**: `service/AutoServiceCircuitBreakerTest` — Resilience4j state transitions
- **Integration**: `controller/AutomobilControllerTest` — `@SpringBootTest` + `WebTestClient`
- **Repository**: `repository/AutoRepositoryTest` — JPA queries, pagination, Hibernate query counting

Test config overrides are in `src/test/resources/application-test.properties` (smaller circuit breaker windows for
faster tests).

## Architecture

**Domain model:**

```
Automobil (id, brand, model, yearMade)
  └── @OneToMany Driver (id, name, surname)
        └── @OneToMany Address (id, street, city)
```

**Layer stack:** `Controller → Service → Repository → JPA Entities`, with `DTO ↔ Entity` mapping via MapStruct.

**Reactive model:** Spring WebFlux (Netty). All service methods return `Mono<>` / `Flux<>`. Blocking JPA calls are
wrapped with `Schedulers.boundedElastic()` and `TransactionTemplate`.

**Key patterns:**

- **Entity Graphs** (`@NamedEntityGraph`) on `Automobil` — two graphs: `Automobil.withDrivers` and
  `Automobil.withDriversAndAddresses` — used to eagerly load nested collections and avoid N+1 queries.

- **Circuit Breaker** (`@CircuitBreaker(name = "dummyClient")` from Resilience4j) wraps the `DummyClient` WebClient
  calls. Configuration lives in `application.properties` under `resilience4j.circuitbreaker.*`.

- **Validation Groups** on `AutomobilDTO`: `OnCreate` / `OnUpdate` — different constraints for POST vs PUT.

- **MapStruct** (`AutomobilMapper`) for compile-time DTO↔Entity conversion; annotation processors for both Lombok and
  MapStruct are declared in `pom.xml`.

- **Global Exception Handling** via `@RestControllerAdvice` in `web/GlobalExceptionHandler` — maps `NotFoundException`
  and validation errors to structured `ErrorResponseDTO`.

- **Kafka** is disabled by default (`kafka.enabled=false`). Enabled via `@ConditionalOnProperty`. For K8s, Redpanda is
  used as the broker (`helm/kafka.yaml`).

- **Observability**: Micrometer + OpenTelemetry OTLP exporter. Disabled locally (
  `management.metrics.export.otlp.enabled=false`), enabled in K8s via ConfigMap. Traces go to Jaeger (
  `helm/jaeger.yaml`). SQL tracing is done via `SqlTracingStatementInspector` (Hibernate `StatementInspector`).

## Database

- H2 in-memory at runtime; schema managed by Flyway (`src/main/resources/db/migration/`).
- `spring.jpa.hibernate.ddl-auto=validate` — Hibernate validates against Flyway-managed schema.
- Migrations: `V1__init.sql` (AUTOMOBIL), `V2__seed_auto.sql` (100 cars), `V3__drivers_addresses.sql` (DRIVER, ADDRESS).

## Key URLs (local)

| URL                                     | Purpose        |
|-----------------------------------------|----------------|
| `http://localhost:8080/auta`            | Main REST API  |
| `http://localhost:8080/swagger-ui.html` | Swagger UI     |
| `http://localhost:8080/v3/api-docs`     | OpenAPI JSON   |
| `http://localhost:8080/actuator/health` | Health check   |
| `http://localhost:8080/h2-console`      | H2 web console |
