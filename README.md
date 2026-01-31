# SynchTask Backend

Backend service for the **SynchTask** platform, focused on authentication,
task management, and real-time collaboration.

This repository contains **only the backend application**, exposed as a REST API
and documented via OpenAPI / Swagger.  
The frontend is developed separately and will be integrated at a later stage.

---

## Overview

The SynchTask Backend is built with **Kotlin** and **Spring Boot**, providing a
robust foundation for collaborative, real-time applications.

It is responsible for:

- authentication and authorization
- core business logic
- real-time communication via WebSockets
- exposing a stable and well-documented API for frontend clients

The project follows an **API-first** approach and is designed to be consumed by
web or mobile clients.

---

## Project Status

SynchTask is an ongoing project.

At this stage, the **backend is considered feature-complete and stable** and is
presented as a standalone **portfolio project**, showcasing backend engineering,
security, and quality practices.

The **frontend is still under active development** and will be integrated once
its implementation is finalized.

---

## Tech Stack

- Kotlin
- Spring Boot 3.x
- MySQL
- Redis
- JWT & OAuth2 (Google)
- WebSockets
- OpenAPI 3 / Swagger
- Gradle
- Detekt
- JaCoCo
- SonarQube

---

## API Documentation

The API is fully documented using OpenAPI 3.

- **Swagger UI**  
  http://localhost:8081/swagger-ui/index.html

- **OpenAPI JSON**  
  http://localhost:8081/v3/api-docs

---

## Getting Started

### Prerequisites

- JDK 17+
- Gradle 8+
- MySQL
- Redis
- `direnv` (recommended for local environment management)

---

### Configuration

Sensitive configuration and secrets are **not committed** to the repository.

All environment-specific values (database credentials, Redis, OAuth, JWT,
SonarQube, etc.) are loaded via **environment variables**.

For local development, the project uses **direnv** with a `.envrc` file
(ignored by Git). This ensures:

- no secrets are committed
- consistent behavior across CLI, IDE, and CI
- clean separation between code and configuration

#### Installing direnv (Linux)

```bash
    sudo apt install direnv
```

After installing, make sure direnv is hooked into your shell (for example zsh)
and allow the project environment:

```bash
    direnv allow
```

Environment variables will then be automatically loaded when entering the
project directory.

### Run Locally

```bash 
    ./gradlew bootRun
```

## Testing & Quality
The application will start and expose the API locally.

### Code Quality (SonarQube)

Start SonarQube locally:

```bash
    docker compose -f docker/docker-compose.sonar.yml up -d
```

Then run the analysis:

```bash

    ./gradlew test                # Run tests
    ./gradlew detekt              # Static analysis
    ./gradlew jacocoTestReport    # Coverage report
    ./gradlew sonarqube           # SonarQube analysis
```

- **SonarQube UI:**      
http://localhost:9001


Stop SonarQube when done:

```bash
    docker compose -f docker/docker-compose.sonar.yml down
```

- Minimum code coverage threshold: 80%
- Continuous inspection via SonarQube
- Quality gates enforced at build level


### Contributing

Contributions are welcome.

Please follow these guidelines:

- write clean, readable, and secure code
- keep public APIs documented
- avoid deprecated APIs
- preserve existing behavior unless explicitly changing it
