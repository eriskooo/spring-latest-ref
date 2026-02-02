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
