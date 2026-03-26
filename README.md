# Spring Latest Reference

Reaktívna CRUD aplikácia — Spring Boot 3.3 · WebFlux · JPA/Hibernate · H2.
Slúži ako referencia pre vzory použité v produkcii: stránkovanie, circuit breaker, tracing, Kafka.

---

## Stack

| Vrstva      | Technológia                               |
|-------------|-------------------------------------------|
| HTTP        | Spring WebFlux (Netty)                    |
| Persistence | Spring Data JPA + Hibernate, H2 in-memory |
| Migrácie    | Flyway                                    |
| Mapping     | MapStruct                                 |
| Resilience  | Resilience4j (circuit breaker)            |
| Messaging   | Spring Kafka (vypnuté predvolene)         |
| Tracing     | Micrometer + OTLP → Jaeger                |
| API docs    | springdoc-openapi (Swagger UI)            |

---

## Doménový model

```
Automobil (id, brand, model, yearMade)
  └── Driver (id, name, surname)
        └── Address (id, street, city)
```

---

## REST API

| Metóda | Endpoint     | Popis                                                 |
|--------|--------------|-------------------------------------------------------|
| GET    | `/auta`      | Zoznam áut (voliteľne `?index=0&offset=10`)           |
| GET    | `/auta/{id}` | Detail auta s vodičmi a adresami                      |
| POST   | `/auta`      | Nové auto                                             |
| PUT    | `/auta/{id}` | Aktualizácia auta                                     |
| DELETE | `/auta/{id}` | Vymazanie auta                                        |
| GET    | `/dummy`     | Test WebClient + circuit breaker (číslo 5 vyhodí 500) |

---

## Spustenie lokálne

```bash
# Java 21 + Maven
mvn spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

---

## Docker

```bash
docker build -t lorma/spring-latest-ref:snapshot .
docker run --rm -p 8080:8080 lorma/spring-latest-ref:snapshot
```

---

## Kubernetes (helm/)

```bash
kubectl apply -f helm/configmap.yaml
kubectl apply -f helm/deployment.yaml
kubectl apply -f helm/service.yaml
kubectl apply -f helm/ingress.yaml   # voliteľné
```

Po zmene ConfigMap treba reštart: `kubectl rollout restart deploy/spring-latest-ref`

---

## Kafka

Predvolene vypnuté. Zapnutie:

```properties
kafka.enabled=true
spring.kafka.bootstrap-servers=localhost:9092
```

Pri štarte aplikácia pošle 1 správu na topic `my.first.topic`, ktorú consumer zaloguje.
V Kubernetes: `kubectl apply -f helm/kafka.yaml` (Redpanda).

---

## Jaeger (tracing)

```bash
kubectl apply -f helm/jaeger.yaml
kubectl port-forward svc/jaeger 16686:16686
# UI: http://localhost:16686  (NodePort: http://localhost:31686)
```

OTLP endpoint je predvolene nakonfigurovaný v `helm/configmap.yaml`:
`management.otlp.tracing.endpoint=http://jaeger:4318/v1/traces`

Sampling = 1.0 (100 %). Pre produkciu znížiť `management.tracing.sampling.probability`.

---

## Testy

```bash
mvn test
```

| Typ                                          | Čo overuje                                                |
|----------------------------------------------|-----------------------------------------------------------|
| Unit (`AutomobilServiceImplTest`)            | Biznis logika služby — CRUD, validácie, NotFoundException |
| Unit (`AutoServiceCircuitBreakerTest`)       | Otvorenie/zatváranie circuit breakera po zlyhaniach       |
| Integration (`AutomobilControllerTest`)      | HTTP endpointy + načítanie vnorených kolekcií             |
| Integration (`AutoRepositoryTest`)           | JPA dotazy, stránkovanie, Hibernate query count           |
| Integration (`ErrorHandlingIntegrationTest`) | Preklad 500 → 503 cez GlobalExceptionHandler              |
