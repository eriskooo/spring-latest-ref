Spring Latest Reference (WebFlux + R2DBC) – Demo aplikácia

Projekt je ukážková reaktívna CRUD aplikácia postavená na Spring Boot 3.3.6 (Spring Framework 6.x, WebFlux) s
perzistenciou cez Spring Data R2DBC do H2 (in‑memory) databázy. Schému a úvodné dáta riadi Flyway. Mapovanie medzi
entitou a DTO je riešené pomocou MapStruct a boilerplate kód je minimalizovaný vďaka Lombok.

Hlavné technológie

- Spring Boot 3.3.6 (Spring 6, WebFlux)
- Spring Data R2DBC (H2)
- Flyway (DB migrácie)
- MapStruct (mapovanie Entity ↔ DTO)
- Lombok (odstránenie boilerplate kódu)
- Actuator (health, info)
- Micrometer tracing OpenTelemetry (voliteľné exporty)

Doménový model a API

- Entita: Automobil (tabuľka AUTOMOBIL)
    - id (Long)
    - brand (String)
    - model (String)
    - yearMade (Integer, stĺpec YEAR_MADE)

- Controller: /api/autos
    - GET /api/autos – zoznam všetkých
    - GET /api/autos/{id} – detail
    - POST /api/autos – vytvorenie
    - PUT /api/autos/{id} – aktualizácia
    - DELETE /api/autos/{id} – odstránenie

Spustenie lokálne

1) Java 21
2) Maven
3) Spustenie:
    - mvn spring-boot:run
    - alebo spustite triedu com.lorman.ref.spring.SpringLatestRefApplication

Konfigurácia (src/main/resources/application.properties)

- JDBC a R2DBC URL pre H2 in‑memory databázu
- Flyway je zapnuté a poukazuje na tú istú H2 DB
- H2 konzola: /h2-console
- Server port: 8080
- Actuator: /actuator (napr. /actuator/health)

Testy

- Repozitár: H2 + Flyway (@SpringBootTest)
- Service: Mockito unit testy
- Controller: WebTestClient overujúci HTTP volanie (RANDOM_PORT)

Poznámky

- V databáze sú po štarte nasadené 3 vzorové záznamy (migration V2__seed_auto.sql).
- K dispozícii je request logging filter pre WebFlux, ktorý loguje metódu, cestu, hlavičky a (textové) telo požiadavky s
  maskovaním citlivých hlavičiek.
