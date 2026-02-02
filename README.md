Spring Latest Reference (WebFlux + R2DBC) – Demo aplikácia

Toto je ukážková reaktívna CRUD aplikácia postavená na Spring Boot 3.3.6 (WebFlux) so
Spring Data R2DBC (H2 in‑memory). Schému a dáta riadi Flyway. Mapovanie zabezpečuje MapStruct a
boilerplate redukuje Lombok.

Anglická verzia: README.en.md

Technológie

- Spring Boot 3.3.6 (Spring 6, WebFlux)
- Spring Data R2DBC (H2)
- Flyway (DB migrácie)
- MapStruct (Entity ↔ DTO)
- Lombok
- Actuator (health, info)

API a doména

- Entita Automobil (tabuľka AUTOMOBIL): id, brand, model, yearMade
- REST:
    - /auta: GET (list), GET /{id}, POST, PUT /{id}, DELETE /{id}
    - /dummy: GET – vráti DummyResponseDTO; ak číslo = 5, vyvolá 500 (spracuje GlobalExceptionHandler)

Chybové spracovanie a logovanie

- GlobalExceptionHandler mapuje bežné chyby na JSON ErrorResponseDTO.
- Outbound WebClient filter globálne prekladá 500 → 503 (Service Unavailable) a zachová správu.
- TraceLogFilter (server-side WebFilter) loguje:
    - „>“ požiadavku (metóda, cesta, hlavičky) a textové body do 2048 znakov,
    - „<“ odpoveď (status, hlavičky) a textové body do 2048 znakov,
    - maskuje citlivé hlavičky (napr. Authorization).

Lokálne spustenie

1) Java 21, Maven
2) mvn spring-boot:run
   alebo spustite com.lorman.ref.spring.SpringLatestRefApplication

Konfigurácia (application.properties)

- H2 R2DBC URL a Flyway nastavenia
- Server port (default 8080)
- Actuator: /actuator/health

Testy

- Integračné: kontroléry, WebClient správanie (preklad 500→503)
- Jednotkové: servisná vrstva

Docker

- build: docker build -t lorma/spring-latest-ref:snapshot .
- run:   docker run --rm -p 8080:8080 lorma/spring-latest-ref:snapshot
- overenie: http://localhost:8080/auta, /actuator/health

Kubernetes (adresár helm/)

- kubectl apply -f helm/configmap.yaml
- kubectl apply -f helm/deployment.yaml
- kubectl apply -f helm/service.yaml
- (voliteľné) kubectl apply -f helm/ingress.yaml

Poznámky

- Po štarte sú vložené 3 záznamy (V2__seed_auto.sql).
- Service je ClusterIP; health-checky cez /actuator/health.
