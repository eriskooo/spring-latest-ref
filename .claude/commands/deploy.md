Run the following steps in order. Stop and report the error if any step fails.

## 1. Build and test

```bash
mvn test
```

All tests must pass before continuing. Report how many tests ran and passed.

## 2. Build Docker image

```bash
mvn clean package -DskipTests
docker build -t lorma/spring-latest-ref:snapshot .
```

Write docker image ID to the report.

## 3. Deploy to Kubernetes

Trigger a rolling restart so the new image is picked up (imagePullPolicy is IfNotPresent with a fixed `snapshot` tag, so
Kubernetes won't detect the change on its own):

```bash
kubectl rollout restart deployment/spring-latest-ref
```

Then wait for the rollout to complete and verify the pod is running:

```bash
kubectl rollout status deployment/spring-latest-ref --timeout=90s
kubectl get pods -l app=spring-latest-ref
```

Report the final pod name and status.
