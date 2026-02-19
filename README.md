# SynchTask Backend

Backend service for **SynchTask**, a collaborative task management platform focused on
**clean architecture, security, and code quality**.

This repository contains only the backend API (Spring Boot + Kotlin) and realtime
actions via WebSockets.

---

## Overview

The backend is responsible for:

- authentication and authorization
- domain/business logic for users, projects, boards, tasks, and chat
- realtime communication via WebSockets
- stable REST API exposure with OpenAPI docs
- code quality gates (tests, static analysis, coverage)

---

## Project Status

- Backend feature-complete baseline (`v0.1-backend-quality`)
- Sonar quality gate integrated
- Coverage baseline above 80%

---

## Tech Stack

- **Language:** Kotlin (JDK 21+)
- **Framework:** Spring Boot 3.x
- **Database:** MySQL (local dev), H2 (tests)
- **Cache / Messaging:** Redis
- **Security:** JWT + OAuth2 (Google)
- **Realtime:** WebSockets
- **Docs:** OpenAPI 3 / Swagger
- **Build:** Gradle
- **Quality:** Detekt, Ktlint, JaCoCo, SonarQube/SonarCloud

---

## Quick Start (Local)

### 1) Prerequisites

- JDK 21+
- Docker (optional, for local services/SonarQube)
- MySQL + Redis (local install or containers)
- `direnv` (optional but recommended)

### 2) Configure environment variables

Create your local env file from template:

```bash
cp .envrc.example .envrc
# edit values for your local environment
```

If you use direnv:

```bash
direnv allow
```

> Never commit secrets. `.envrc` is ignored by Git.

### 3) Run the application

```bash
./gradlew bootRun
```

Default docs endpoints:

- Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

---

## API Testing (Bruno)

Bruno collections are maintained as `.bru` files in:

- `docs/bruno/auth`
- `docs/bruno/security`
- `docs/bruno/smoke`
- `docs/bruno/environments`

Use `docs/bruno/bruno.json` as the collection metadata file.

---

## Testing & Quality

Run the main local checks:

```bash
./gradlew test
./gradlew detekt
./gradlew jacocoTestReport
```

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

## Architecture & Data Model

Main aggregate roots:

- User
- Board
- Project
- ChatRoom

Detailed docs live in [`docs/README.md`](docs/README.md).

---

## Contributing

Contributions are welcome. Keep changes:

- clean and readable
- aligned with architectural boundaries
- documented when public API behavior changes

