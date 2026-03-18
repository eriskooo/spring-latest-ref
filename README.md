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
  - Tracing: Kafka producer/consumer spans are enabled via Micrometer Observation and exported to Jaeger
    when OTLP tracing is configured. In Jaeger you will see spans like "kafka.producer" and "kafka.consumer",
    linked together via trace context headers.
- Kubernetes: manifests in helm/ provide an in‑cluster Redpanda (helm/kafka.yaml) and configure the app via env vars:
    - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    - KAFKA_ENABLED=true
  - Tracing is enabled by default; after the Pod starts it will produce a message which is then consumed by the
    listener. In Jaeger UI select service "spring-latest-ref" and look for spans labeled kafka.producer/kafka.consumer.
- Health checks: Kubernetes probes use dedicated Actuator endpoints (/actuator/health/liveness,
  /actuator/health/readiness).
  Kafka health contributor is disabled by default in helm/configmap.yaml to keep readiness independent from Kafka;
  enable with management.health.kafka.enabled=true if desired.

Run locally

1) Java 21 and Maven
2) mvn spring-boot:run
   or run com.lorman.ref.spring.SpringLatestRefApplication

API docs (OpenAPI / Swagger UI)

- Swagger UI (available by default):
    - http://localhost:8080/swagger-ui.html
    - http://localhost:8080/swagger-ui/index.html (the UI is served here; /swagger-ui.html redirects)
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml

Notes and troubleshooting

- Security: these endpoints are publicly accessible (no token needed). See SecurityConfig permitAll for
  /v3/api-docs, /v3/api-docs.yaml, /v3/api-docs/**, /swagger-ui.html, /swagger-ui/**.
- If you get 404:
    - Ensure the app is running on port 8080 (or adjust the URL if you changed server.port).
    - Check that the springdoc starter is present (pom.xml contains springdoc-openapi-starter-webflux-ui).
    - If you use a custom context path (server.servlet.context-path or spring.webflux.base-path), prefix the URLs, e.g.,
      http://localhost:8080/myapp/swagger-ui.html.

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

Jaeger (trasovanie) – nasadenie a použitie

- Nasadiť Jaeger (all-in-one) do klastru:
    - kubectl apply -f helm/jaeger.yaml
- Export trace z aplikácie do Jaeger cez OTLP HTTP (4318) je v Helm ConfigMap už POVOLENÝ predvolene
  (management.otlp.tracing.endpoint=http://jaeger:4318/v1/traces). Ak si ConfigMap menil v minulosti, uisti sa, že
  tento riadok tam je a zmeny aplikuj:
    - kubectl apply -f helm/configmap.yaml && kubectl rollout restart deploy/spring-latest-ref
- Prístup do Jaeger UI:
    - Možnosť A – port-forward (univerzálne):
        - kubectl port-forward svc/jaeger 16686:16686
        - otvoriť http://localhost:16686
    - Možnosť B – NodePort (bez port-forward):
        - nie je potrebné aplikovať extra manifest – NodePort služba je súčasťou helm/jaeger.yaml
        - otvoriť http://localhost:31686
- Poznámky:
    - Sampling je v demu nastavený na 1.0 (všetko). Pre produkciu znížte management.tracing.sampling.probability.
    - Ak používate inú inštaláciu Jaegera/OTLP kolektora, nastavte endpoint podľa vašej služby.
    - Ak na http://localhost:16686 nič nevidíš:
        - skontroluj, že beží port-forward (prípadne použi NodePort možnosť B),
        - over, že Pod "jaeger" je v stave Running: kubectl get pods -l app=jaeger,
        - ak používaš Minikube: minikube service jaeger-nodeport --url (vypíše presnú URL).
    - JDBC logovanie do spanov:
        - SQL dotazy sa pridávajú ako eventy s názvom "jdbc.query" do aktuálneho spanu (atribút db.statement obsahuje
          SQL šablónu).
        - Lokálne je to zapnuté cez Hibernate StatementInspector v application.properties:
            -
            spring.jpa.properties.hibernate.session_factory.statement_inspector=com.lorman.ref.spring.observation.SqlTracingStatementInspector
        - V Kubernetes je to zapnuté v helm/configmap.yaml v časti application.properties.
        - Ako to vypnúť: zmaž alebo zakomentuj uvedený property riadok.
    - Ak v Jaeger UI vidíš len jednu službu ("jaeger all-in-one"):
        - je to normálne, pokiaľ tvoja aplikácia ešte neposlala žiadne spany;
        - po nasadení/aktualizácii ConfigMap vyvolaj traffic na aplikáciu (napr. viackrát GET na /auta cez
          NodePort http://localhost:31301/auta),
        - v UI zvoľ rozsah času „Last 1 hour“ a v zozname služieb vyhľadaj „spring-latest-ref“;
        - over, že Pod aplikácie načítal konfiguráciu s OTLP endpointom: kubectl exec -it deploy/spring-latest-ref --
          cat /etc/spring-latest-ref-config/application.properties | grep otlp
        - skontroluj logy aplikácie pre prípadné chyby exportu OTLP (401/404/connection refused): kubectl logs
          deploy/spring-latest-ref | findstr /C:"otlp" /C:"OpenTelemetry" /C:"export"
        - uisti sa, že premenná prostredia OTEL_RESOURCE_ATTRIBUTES je nastavená na service.name=spring-latest-ref (je
          pridaná v helm/deployment.yaml),
        - ak stále nič, reštartuj deployment: kubectl rollout restart deploy/spring-latest-ref a znova vygeneruj
          traffic.
    - Ak v Jaeger UI nevidíš žiadne traces:
        - over, že Deployment bol reštartovaný po úprave ConfigMap: kubectl rollout restart deploy/spring-latest-ref,
        - v Podoch je pripojený konfig súbor s endpointom (pozri /etc/spring-latest-ref-config/application.properties),
        - vyvolaj traffic (napr. opakovane GET na /auta),
        - v Jaeger UI vyber správny časový rozsah (napr. Last 1 hour) a službu "spring-latest-ref".
  - Kafka spany v Jaegeri:
      - Lokálne: nastav kafka.enabled=true a spring.kafka.bootstrap-servers, spustenie aplikácie odošle 1 správu,
        ktorú consumer spracuje. V Jaegeri uvidíš "kafka.producer" a "kafka.consumer" spany.
      - V Kubernetes: po nasadení helm/kafka.yaml a deploymentu aplikácie sa správa odošle a spracuje automaticky.
      - Ak ich nevidíš, over:
          - že Kafka beží (topic môže byť vytvorený automaticky Redpanda/Kafka),
          - že v konfigurácii je zapnutý observation pre Kafka (v projekte je predvolene zapnutý),
          - logy aplikácie pre chyby pripojenia na Kafka broker (Connection refused, authorization atď.).

FAQ – ConfigMap a reštart Podu

- Otázka: Na aktualizáciu ConfigMap mám reštartnúť Pod?
- Odpoveď: Áno, pre túto aplikáciu áno. Hoci Kubernetes aktualizuje obsah ConfigMap volume v súbore, Spring Boot
  štandardne nenahrá nové hodnoty externých vlastností za behu (pokiaľ nepoužijete mechanizmus ako
  spring-cloud-kubernetes-config s automatickým reloadom). Aby sa nové hodnoty z ConfigMap prejavili v aplikácii, je
  potrebný rolling reštart Deploymentu.
- Odporúčaný postup po úprave helm/configmap.yaml:
    - kubectl apply -f helm/configmap.yaml
    - kubectl rollout restart deploy/spring-latest-ref
    - voliteľne skontroluj priebeh: kubectl rollout status deploy/spring-latest-ref
- Overenie, že Pod načítal nové hodnoty:
    - kubectl exec -it deploy/spring-latest-ref -- cat /etc/spring-latest-ref-config/application.properties | findstr
      otlp
- Tip: Ak chcete dosiahnuť reštart bez manuálneho príkazu, môžete zmeniť (anotáciou) špecifikáciu Deploymentu, čo vyvolá
  nový rollout. V praxi je však príkaz "kubectl rollout restart" najjednoduchší a najčitateľnejší.

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

- OpenAPI UI: already included
    - The project already includes the dependency:
      org.springdoc:springdoc-openapi-starter-webflux-ui:2.6.0
    - Visit /swagger-ui.html and /v3/api-docs (see links above).

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
