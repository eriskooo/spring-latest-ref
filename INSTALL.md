# Inštalačná príručka pre vývojára (Windows 11)

Táto príručka ťa prevedie celým nastavením vývojárskeho prostredia od nuly — od čistého Windows 11 až po bežiaci projekt
lokálne aj v Kubernetes.

---

## Obsah

1. [Potrebné nástroje](#1-potrebné-nástroje)
2. [Inštalácia Java 21](#2-inštalácia-java-21)
3. [Inštalácia Maven](#3-inštalácia-maven)
4. [Inštalácia Git](#4-inštalácia-git)
5. [Inštalácia Docker Desktop](#5-inštalácia-docker-desktop)
6. [Povolenie Kubernetes v Docker Desktop](#6-povolenie-kubernetes-v-docker-desktop)
7. [Inštalácia NGINX Ingress Controllera](#7-inštalácia-nginx-ingress-controllera)
8. [IDE — IntelliJ IDEA](#8-ide--intellij-idea)
9. [Klonovanie projektu](#9-klonovanie-projektu)
10. [Spustenie lokálne (bez Dockera)](#10-spustenie-lokálne-bez-dockera)
11. [Spustenie cez Docker](#11-spustenie-cez-docker)
12. [Nasadenie do Kubernetes](#12-nasadenie-do-kubernetes)
13. [Overenie funkčnosti](#13-overenie-funkčnosti)

---

## 1. Potrebné nástroje

| Nástroj        | Verzia   | Na čo slúži                                 |
|----------------|----------|---------------------------------------------|
| Java (JDK)     | 21       | Beh a kompilácia Spring Boot aplikácie      |
| Maven          | 3.9+     | Build systém — kompilácia, testy, packaging |
| Git            | aktuálna | Verziovanie kódu a klonovanie repozitára    |
| Docker Desktop | aktuálna | Kontajnerizácia + vstavaný Kubernetes       |
| IntelliJ IDEA  | aktuálna | IDE — odporúčané pre Spring Boot vývoj      |

> **Tip:** Všetky nástroje si nainštaluj v poradí, v akom sú v tejto príručke.

---

## 2. Inštalácia Java 21

Projekt vyžaduje **Java 21 (LTS)**. Odporúčame distribúciu **Eclipse Temurin** od Adoptium — je zadarmo a open-source.

1. Choď na https://adoptium.net
2. Vyber **Temurin 21 (LTS)**, Windows x64, `.msi` inštalátor
3. Spusti inštalátor — zaškrtni možnosť **"Set JAVA_HOME variable"** a **"Add to PATH"**
4. Overie inštaláciu otvorením nového terminálu (cmd alebo PowerShell):
   ```
   java -version
   ```
   Výstup by mal obsahovať `openjdk version "21..."`

---

## 3. Inštalácia Maven

Maven sa stará o stiahnutie knižníc, kompiláciu a testy.

1. Choď na https://maven.apache.org/download.cgi
2. Stiahni **Binary zip archive** (napr. `apache-maven-3.9.x-bin.zip`)
3. Rozbaľ do priečinka, napr. `C:\tools\maven`
4. Pridaj Maven do systémovej PATH:
    - Otvor **Nastavenia systému** → hľadaj „Premenné prostredia"
    - V sekcii „Systémové premenné" nájdi `Path` → klikni **Upraviť**
    - Pridaj nový riadok: `C:\tools\maven\bin`
5. Overie v novom termináli:
   ```
   mvn -version
   ```
   Výstup by mal obsahovať `Apache Maven 3.9...`

> **Alternatíva:** Ak máš nainštalovaný [Chocolatey](https://chocolatey.org), môžeš použiť:
> ```
> choco install maven
> ```

---

## 4. Inštalácia Git

1. Choď na https://git-scm.com/download/win
2. Stiahni a spusti inštalátor
3. Počas inštalácie ponechaj predvolené možnosti
4. Overie v termináli:
   ```
   git --version
   ```

---

## 5. Inštalácia Docker Desktop

Docker Desktop obsahuje Docker aj vstavaný Kubernetes — nainštaluješ teda obe veci naraz.

**Požiadavky pred inštaláciou:**

- Windows 11 s povolenou virtualizáciou (WSL 2)
- Aspoň 8 GB RAM (16 GB odporúčané)

**Postup:**

1. Choď na https://www.docker.com/products/docker-desktop/
2. Stiahni **Docker Desktop for Windows**
3. Spusti inštalátor — ponechaj predvolené možnosti vrátane **WSL 2 backend**
4. Po inštalácii reštartuj počítač
5. Spusti Docker Desktop (ikonka v systray)
6. Overie v termináli:
   ```
   docker --version
   docker run hello-world
   ```
   Ak vidíš `Hello from Docker!`, všetko funguje.

> **Čo je WSL 2?** Windows Subsystem for Linux 2 — umožňuje Dockeru bežať efektívne na Windowse. Docker Desktop ho
> nainštaluje automaticky.

---

## 6. Povolenie Kubernetes v Docker Desktop

Docker Desktop obsahuje jednouzlový Kubernetes cluster — ideálny na lokálny vývoj.

1. Otvor **Docker Desktop**
2. Klikni na **Settings** (ozubené koliesko vpravo hore)
3. Vyber záložku **Kubernetes**
4. Zaškrtni **Enable Kubernetes**
5. Klikni **Apply & Restart** — prvé spustenie trvá niekoľko minút (stiahne potrebné obrazy)
6. Overie v termináli:
   ```
   kubectl get nodes
   ```
   Výstup by mal zobraziť jeden uzol so stavom `Ready`.

> **Čo je kubectl?** Nástroj príkazového riadku na ovládanie Kubernetes clustera. Docker Desktop ho nainštaluje
> automaticky a nastaví pripojenie na lokálny cluster.

---

## 7. Inštalácia NGINX Ingress Controllera

Ingress controller umožňuje prístup k aplikácii cez HTTP z tvojho prehliadača.

Spusti v termináli:

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.1/deploy/static/provider/cloud/deploy.yaml
```

Počkaj, kým sa ingress controller naštartuje (asi 1–2 minúty):

```bash
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=120s
```

Overie:

```bash
kubectl get pods -n ingress-nginx
```

Mal by si vidieť pod s názvom `ingress-nginx-controller-...` so stavom `Running`.

---

## 8. IDE — IntelliJ IDEA

Odporúčame **IntelliJ IDEA Community Edition** (zadarmo) alebo Ultimate (platená, lepšia podpora Spring).

1. Choď na https://www.jetbrains.com/idea/download/
2. Stiahni a nainštaluj **Community** alebo **Ultimate**
3. Po otvorení IDE nainštaluj plugin **Lombok** (ak nie je súčasťou inštalácie):
    - `File → Settings → Plugins` → hľadaj `Lombok` → Install

> **Prečo Lombok?** Projekt používa Lombok na automatické generovanie getterov, setterov a konštruktorov. Bez pluginu ti
> IDE bude ukazovať falošné chyby.

---

## 9. Klonovanie projektu

```bash
git clone <URL repozitára>
cd spring-latest-ref
```

Otvor projekt v IntelliJ IDEA:

- `File → Open` → vyber priečinok `spring-latest-ref`
- IDE automaticky rozozná Maven projekt a stiahne závislosti

---

## 10. Spustenie lokálne (bez Dockera)

Toto je najrýchlejší spôsob pri vývoji — aplikácia beží priamo na tvojom počítači.

```bash
# Stiahni závislosti a skompiluj
mvn clean package -DskipTests

# Spusti aplikáciu
mvn spring-boot:run
```

Aplikácia sa spustí na http://localhost:8080

**Spustenie testov:**

```bash
mvn test
```

> **Čo sa deje na pozadí?** Aplikácia používa H2 in-memory databázu — nepotrebuješ žiadny externý databázový server.
> Schéma sa vytvorí automaticky pri štarte cez Flyway migrácie.

---

## 11. Spustenie cez Docker

Najprv zostav Docker image:

```bash
docker build -t lorma/spring-latest-ref:snapshot .
```

> **Poznámka:** Build môže trvať niekoľko minút pri prvom spustení, pretože Docker stiahne základné image a Maven
> závislosť. Pri ďalšom build-e bude rýchlejší vďaka cache.

Spusti kontajner:

```bash
docker run --rm -p 8080:8080 lorma/spring-latest-ref:snapshot
```

Aplikácia je dostupná na http://localhost:8080

---

## 12. Nasadenie do Kubernetes

Kubernetes nasadenie obsahuje samotnú aplikáciu, Kafka (Redpanda) broker a Jaeger pre sledovanie trás (tracing).

### Krok 1 — Zostav Docker image

Kubernetes v Docker Desktop má prístup k lokálnym Docker images, takže nie je potrebné žiadne registry:

```bash
docker build -t lorma/spring-latest-ref:snapshot .
```

### Krok 2 — Nasaď Jaeger (tracing)

```bash
kubectl apply -f helm/jaeger.yaml
```

### Krok 3 — Nasaď Kafka (Redpanda)

```bash
kubectl apply -f helm/kafka.yaml
```

### Krok 4 — Nasaď konfiguráciu aplikácie

```bash
kubectl apply -f helm/configmap.yaml
```

### Krok 5 — Nasaď aplikáciu

```bash
kubectl apply -f helm/deployment.yaml
kubectl apply -f helm/service.yaml
kubectl apply -f helm/ingress.yaml
```

### Krok 6 — Overie beh podov

```bash
kubectl get pods
```

Čakaj, kým všetky pody majú stav `Running`:

```
NAME                                READY   STATUS    RESTARTS   AGE
spring-latest-ref-xxx               1/1     Running   0          1m
kafka-xxx                           1/1     Running   0          2m
jaeger-xxx                          1/1     Running   0          2m
```

> **Tip:** Ak pod zostáva v stave `Pending` alebo `CrashLoopBackOff`, pozri logy:
> ```bash
> kubectl logs <nazov-podu>
> kubectl describe pod <nazov-podu>
> ```

---

## 13. Overenie funkčnosti

### Lokálne (Maven alebo Docker)

| URL                                   | Čo uvidíš             |
|---------------------------------------|-----------------------|
| http://localhost:8080/auta            | Zoznam áut (JSON)     |
| http://localhost:8080/swagger-ui.html | Swagger UI            |
| http://localhost:8080/v3/api-docs     | OpenAPI dokumentácia  |
| http://localhost:8080/actuator/health | Stav aplikácie        |
| http://localhost:8080/h2-console      | H2 databázová konzola |

### Kubernetes

| URL                              | Čo uvidíš                |
|----------------------------------|--------------------------|
| http://localhost/auta            | Zoznam áut (cez Ingress) |
| http://localhost/actuator/health | Stav aplikácie           |
| http://localhost:31686           | Jaeger UI (tracing)      |

> **Prečo iné porty v K8s?** V Kubernetes aplikácia nie je priamo na porte 8080 tvojho PC — Ingress controller prijíma
> požiadavky na porte 80 a preposiela ich do aplikácie. Jaeger je dostupný cez NodePort 31686.

### Rýchly test API

```bash
# Zoznam áut
curl http://localhost:8080/auta

# Detail auta s ID 1
curl http://localhost:8080/auta/1

# Nové auto
curl -X POST http://localhost:8080/auta \
  -H "Content-Type: application/json" \
  -d '{"brand":"Skoda","model":"Octavia","yearMade":2020}'
```

---

## Časté problémy

**`JAVA_HOME` nie je nastavené:**
Overie cez `echo %JAVA_HOME%`. Ak je prázdne, nastav ho manuálne v systémových premenných na cestu k JDK (napr.
`C:\Program Files\Eclipse Adoptium\jdk-21...`).

**Docker Desktop sa nespustí / WSL 2 chyba:**
Spusti v PowerShell ako administrátor:

```powershell
wsl --update
wsl --set-default-version 2
```

**`kubectl` nerozozná cluster:**

```bash
kubectl config get-contexts
kubectl config use-context docker-desktop
```

**Port 8080 je obsadený:**

```bash
# Zisti, ktorý proces používa port 8080
netstat -ano | findstr :8080
# Ukonči proces podľa PID
taskkill /PID <cislo> /F
```

**Pody v K8s zostávajú `Pending`:**
Pravdepodobne Docker Desktop nemá dostatok pamäte. Nastav aspoň 4 GB RAM pre Docker:
`Docker Desktop → Settings → Resources → Memory → 4 GB`
