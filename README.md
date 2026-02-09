Spring Latest Reference (WebFlux + R2DBC) – Demo app

This is a reactive CRUD demo built on Spring Boot 3.3.6 (WebFlux) with Spring Data R2DBC (H2 in‑memory).
Flyway manages schema and seed data. MapStruct handles mapping; Lombok reduces boilerplate.

Technologies

- Spring Boot 3.3.6 (Spring 6, WebFlux)
- Spring Data R2DBC (H2)
- Flyway (DB migrations)
- MapStruct (Entity ↔ DTO)
- Lombok
- Actuator (health, info)

API and domain

- Entity: Automobil (table AUTOMOBIL) — id, brand, model, yearMade
- REST endpoints:
    - /auta: GET (list), GET /{id}, POST, PUT /{id}, DELETE /{id}
    - /dummy: GET — returns DummyResponseDTO; when number=5 it throws 500 handled by GlobalExceptionHandler

Error handling and logging

- GlobalExceptionHandler maps common errors to JSON ErrorResponseDTO.
- Global WebClient filter translates upstream 500 → 503 (Service Unavailable) and preserves the original message.
- TraceLogFilter (server‑side WebFilter) logs:
    - ">" request (method, path, headers) and textual bodies up to 2048 chars,
    - "<" response (status, headers) and textual bodies up to 2048 chars,
    - masks sensitive headers (e.g., Authorization).

Kafka (brief)

- Feature flag: kafka.enabled=false by default (no broker required for local dev and tests).
- When enabled, the app provides:
    - KafkaStartupProducer: sends one message to topic "my.first.topic" at application startup.
    - KafkaMessageConsumer: listens on "my.first.topic" (group: spring-latest-ref-group) and logs payloads.
- How to enable locally:
    - Start a Kafka/Redpanda broker and set in application.properties:
        - kafka.enabled=true
        - spring.kafka.bootstrap-servers=localhost:9092
- Kubernetes: manifests in helm/ provide an in‑cluster Redpanda (helm/kafka.yaml) and configure the app via env vars:
    - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    - KAFKA_ENABLED=true
- Health checks: Kubernetes probes use dedicated Actuator endpoints (/actuator/health/liveness,
  /actuator/health/readiness).
  Kafka health contributor is disabled by default in helm/configmap.yaml to keep readiness independent from Kafka;
  enable with management.health.kafka.enabled=true if desired.

Run locally

1) Java 21 and Maven
2) mvn spring-boot:run
   or run com.lorman.ref.spring.SpringLatestRefApplication

Configuration (application.properties)

- H2 R2DBC URL and Flyway settings
- Server port (default 8080)
- Actuator: /actuator/health
- Optional Kafka settings (see Kafka section)

Tests

- Integration: controllers and WebClient behavior (500→503 translation)
- Unit: service layer

Docker

- build: docker build -t lorma/spring-latest-ref:snapshot .
- run:   docker run --rm -p 8080:8080 lorma/spring-latest-ref:snapshot
- verify: http://localhost:8080/auta, /actuator/health

Kubernetes (helm/ directory)

- kubectl apply -f helm/configmap.yaml
- kubectl apply -f helm/deployment.yaml
- kubectl apply -f helm/service.yaml
- (optional) kubectl apply -f helm/ingress.yaml

Notes

- Three sample rows are inserted on startup (V2__seed_auto.sql).
- Service type is ClusterIP; probes use dedicated Actuator endpoints.

What else to add (proposal checklist)

The app already demonstrates CRUD, WebFlux/WebClient, Kafka, and error handling. Below is a pragmatic, production‑minded
checklist you can pick from. Items are grouped and include suggested starters/libraries.

- API design and UX
    - OpenAPI/Swagger docs and UI (springdoc-openapi-starter-webflux-ui) for discoverability and client generation.
    - Validation groups and Problem Details (RFC 7807) responses consistently across endpoints.
    - Pagination, filtering, and sorting conventions for list endpoints; ensure stable sorting and total counts.
    - API versioning strategy (URI or header based) and deprecation headers.

- Security
    - Spring Security (stateless) with JWT or OAuth2 Resource Server (spring-boot-starter-oauth2-resource-server).
    - Method‑level security for service layer; security test slices.
    - Secrets management via environment/Pod mounts; avoid committing secrets. Support external vaults (e.g., HashiCorp
      Vault, AWS Secrets Manager) where applicable.

- Resilience and performance
    - Resilience4j for circuit breaker, retry, rate limiter, bulkhead on outbound WebClient calls.
    - Caching layer (spring-boot-starter-cache) with Caffeine for in‑memory and Redis for distributed cache.
    - Idempotency for POST endpoints (idempotency‑key header with short‑lived store, e.g., Redis) to handle retries
      safely.
    - Request rate limiting (e.g., Bucket4j or Spring Cloud Gateway if introducing an edge layer).

- Data and messaging robustness
    - Transactional outbox or Debezium CDC for reliable “DB ➜ Kafka” emission (avoid dual‑write problems).
    - Message schema governance (Confluent/Redpanda Schema Registry + Avro/JSON Schema) and compatibility rules.
    - Soft deletes and audit fields (createdBy, createdAt, updatedBy, updatedAt); database indexes for query hotspots.
    - Database migrations per environment profiles, repeatable migrations; ensure Flyway clean is disabled outside dev.

- Observability
    - Extend Micrometer/OTel setup with exemplars and baggage propagation; enrich logs with trace/span ids (already
      partially covered by logstash encoder).
    - Add custom business metrics (Counter/Gauge/Timer/LongTaskTimer) for key flows (e.g., proposals created/approved).
    - Log sanitization policy and PII handling rules; structured error logs with error codes.

- Operations and runtime
    - Graceful shutdown tuning and reactive backpressure limits; connection/timeouts for WebClient.
    - Health contributors for external deps (DB, Kafka, Redis, remote services) split into liveness/readiness.
    - Feature flags (e.g., Togglz or Unleash) to toggle new capabilities without redeploys.
    - Scheduling (Spring @Scheduled) or Spring Batch for periodic/reconciliation jobs where needed.

- Testing and quality gates
    - Testcontainers for Kafka/DB integration tests to remove external deps during CI.
    - Contract tests (Spring Cloud Contract) or Pact for provider/consumer verification across teams.
    - Mutation testing (PIT) or coverage thresholds; static analysis (SpotBugs/Checkstyle) and formatting (Spotless).

- Delivery and platform
    - CI/CD: Maven wrapper already present; add GitHub Actions/GitLab CI pipeline with build, tests, SBoM (CycloneDX),
      image build, Trivy scan, and Helm chart linting.
    - Docker: multi‑stage build and distroless/base‑image hardening; read‑only root FS, non‑root user.
    - Kubernetes: resource limits/requests, PodSecurity standards, network policies, HPA (CPU/RPS based),
      PodDisruptionBudget, startup/shutdown probes, and config via ConfigMap/Secret already started here.
    - Centralized config (Spring Cloud Config) if multiple services share config.

Getting started with a few high‑impact additions

- OpenAPI UI: add dependency
    - <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
        <version>2.6.0</version>
      </dependency>
    - Then visit /swagger-ui.html and /v3/api-docs.

- Resilience4j: add
    - <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-spring-boot3</artifactId>
      </dependency>
    - Wrap WebClient calls with circuit breaker/retry; expose metrics to Micrometer.

- Caching (Caffeine): add
    - <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
      </dependency>
    - <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
      </dependency>
    - Enable with @EnableCaching and annotate hot read paths.

Pick selectively according to your proposal app’s scope (e.g., if it processes critical requests, prioritize security,
resilience, and observability; if high read traffic, prioritize caching and pagination).
