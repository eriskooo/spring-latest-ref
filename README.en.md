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

Error handling and HTTP translation

- GlobalExceptionHandler maps common errors to JSON ErrorResponseDTO.
- Global WebClient filter translates upstream 500 → 503 (Service Unavailable) and preserves the original message.
- TraceLogFilter (server‑side WebFilter) logs:
    - ">" request (method, path, headers) and textual bodies up to 2048 chars,
    - "<" response (status, headers) and textual bodies up to 2048 chars,
    - masks sensitive headers (e.g., Authorization).

Run locally

1) Java 21 and Maven
2) mvn spring-boot:run
   or run com.lorman.ref.spring.SpringLatestRefApplication

Configuration (application.properties)

- H2 R2DBC URL and Flyway settings
- Server port (default 8080)
- Actuator: /actuator/health

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
- Service type is ClusterIP; health checks use /actuator/health.
