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

Docker (najjednoduchší spôsob)

- Predpoklady: nainštalovaný Docker Desktop (alebo kompatibilný Docker engine).
- Build Docker image v koreňovom adresári projektu:
  docker build -t lorma/spring-latest-ref:snapshot .
- Spustenie kontajnera lokálne na porte 8080:
  docker run --rm -p 8080:8080 lorma/spring-latest-ref:snapshot
- Overenie:
    - http://localhost:8080/auta
    - http://localhost:8080/actuator/health
- Voliteľné: push do registry (nahraď „your-registry“):
  docker tag lorma/spring-latest-ref:snapshot your-registry/spring-latest-ref:snapshot
  docker push your-registry/spring-latest-ref:snapshot

Kubernetes – jednoduché nasadenie

Poznámka: V adresári helm/ sú pripravené jednoduché YAML manifesty (ConfigMap, Deployment, Service, Ingress). Nie je
potrebné používať Helm CLI – stačí kubectl apply.

1) Priprav image
    - Možnosť A – Docker Desktop Kubernetes: stačí lokálne vybuildovaný image s rovnakým názvom ako v deployment.yaml (
      lorma/spring-latest-ref:snapshot). ImagePullPolicy je IfNotPresent, takže klaster použije lokálny image.
    - Možnosť B – iný klaster: pushni image do vašej registry a uprav „spec.template.spec.containers[0].image“ v
      helm/deployment.yaml.

2) Aplikuj manifesty (poradie nie je kritické, odporúčané):
   kubectl apply -f helm/configmap.yaml
   kubectl apply -f helm/deployment.yaml
   kubectl apply -f helm/service.yaml
   # voliteľne, ak máš Ingress Controller (napr. nginx):
   kubectl apply -f helm/ingress.yaml

3) Over stav
   kubectl get pods
   kubectl get svc spring-latest-ref
   kubectl logs deploy/spring-latest-ref

4) Prístup k službe
    - Najjednoduchšie cez port-forward (funguje všade):
      kubectl port-forward svc/spring-latest-ref 8080:8080
      a otvor http://localhost:8080/auta alebo /actuator/health
    - Alebo cez Ingress (ak máš nainštalovaný Ingress Controller). V tom prípade použij adresy definované v
      helm/ingress.yaml (paths /auta a /actuator).

5) Odstránenie
   kubectl delete -f helm/ingress.yaml --ignore-not-found
   kubectl delete -f helm/service.yaml
   kubectl delete -f helm/deployment.yaml
   kubectl delete -f helm/configmap.yaml

Poznámky ku K8s manifestom

- Service je typu ClusterIP (vhodné pre Ingress a port-forward). Ak chceš prístup bez Ingressu, môžeš dočasne zmeniť typ
  na NodePort.
- Health-checky v Deployment používajú Spring Boot Actuator endpoint /actuator/health.
- ConfigMap (helm/configmap.yaml) pridáva vlastný application.properties do kontajnera a je pripojený do
  /etc/spring-latest-ref-config; načítanie je umožnené premennou SPRING_CONFIG_ADDITIONAL_LOCATION.
