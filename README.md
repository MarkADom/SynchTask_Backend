# SynchTask Backend

![CI workflow](docs/assets/badge-ci.svg)
![OpenAPI](docs/assets/badge-openapi.svg)
![Bruno API tests](docs/assets/badge-bruno.svg)

Spring Boot 3 + Kotlin backend for SynchTask.

## Tech stack

- Kotlin + Spring Boot 3
- MySQL + Redis
- JWT/OAuth2 security
- OpenAPI / Swagger UI
- Bruno integration test suites

## Run locally

1. Configure environment variables:


```bash
cp .envrc.example .envrc
# edit values for your local environment
```

If you use direnv:

```bash
direnv allow
```


2. Run the application

```bash
./gradlew bootRun
```

3. Run with dev profile:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun`
```

4. Open API docs:

- Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

---

## Testing & Quality

Run the main local checks:

```bash
./gradlew test
./gradlew detekt
./gradlew jacocoTestReport
```
---

### SonarQube local (optional)

Start SonarQube:

```bash
docker compose -f docker/docker-compose.sonar.yml up -d
```

Run analysis:

```bash
./gradlew sonar
```

Stop SonarQube:

```bash
docker compose -f docker/docker-compose.sonar.yml down
```

- SonarQube UI: http://localhost:9001/projects
---

## API Testing (Bruno)

For full configuration and profile notes, see [`docs/configuration.md`](docs/configuration.md).
Bruno collections live in [`bruno/`](bruno).
Run instructions and suite details are in [`bruno/README.md`](bruno/README.md).

---

## Architecture & Data Model

Main aggregate roots:

- User
- Board
- Project
- ChatRoom

Detailed docs live in [`docs/README.md`](docs/README.md).

---

## Docs map

- Documentation index: [`docs/README.md`](docs/README.md)
- Architecture overview: [`docs/architecture.md`](docs/architecture.md)
- Database notes and ER diagram: [`docs/dataBase/`](docs/dataBase)
- OpenAPI export and usage: [`docs/openapi/`](docs/openapi)
- Documentation audit for v1.0: [`docs/documentation-audit.md`](docs/documentation-audit.md)

---

## Contributing

Contributions are welcome. Keep changes:

- clean and readable
- aligned with architectural boundaries
- documented when public API behavior changes

---

## License

Licensed under the terms in [`LICENSE.md`](LICENSE.md).
