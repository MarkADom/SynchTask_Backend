# SynchTask Backend

Backend service for **SynchTask**, a collaborative task management platform
designed with a strong focus on **clean architecture, security, and code quality**.

This repository contains **only the backend application**, exposed as a REST API
and supporting real-time features via WebSockets.  
The frontend is developed separately and will be integrated in a later phase.

---

## Overview

The **SynchTask Backend** is built with **Kotlin** and **Spring Boot**, following
modern backend engineering practices and an **API-first** approach.

It is responsible for:

- authentication and authorization
- core domain and business logic
- real-time communication via WebSockets
- exposing a stable, well-documented REST API
- enforcing strict quality and security standards

This project is intentionally structured as a **portfolio-grade backend**, aiming
to demonstrate production-level practices rather than a minimal demo.

---

## Project Status

 **Backend stable and feature-complete**  
 **Quality Gate passed (SonarQube)**  
 **83% test coverage on overall codebase**

The backend is currently frozen as a **quality baseline milestone**
(`v0.1-backend-quality`) and considered ready for real-world integration.

The frontend is under active development and will be integrated once finalized.

---

## Data Model & Architecture

SynchTask uses a relational database (MySQL/PostgreSQL) with a domain-driven design.

The data model is structured around clear Aggregate Roots:
- User
- Board
- Project
- ChatRoom

Each aggregate defines a strict consistency boundary and is primarily accessed via its own repository.

Detailed ER diagram and aggregate documentation are available in the `/docs` directory.

Cross-aggregate access is intentionally avoided at the repository level to preserve domain integrity.

---

## Architecture & Principles

- Clean Architecture (clear separation of concerns)
- Application / Domain / Infrastructure layers
- API-first design
- Explicit boundaries between business logic and frameworks
- Test-driven mindset focused on meaningful coverage

---

## Tech Stack

- **Language:** Kotlin
- **Framework:** Spring Boot 3.x
- **Database:** MySQL
- **Cache / Messaging:** Redis
- **Security:** JWT, OAuth2 (Google)
- **Real-time:** WebSockets
- **API Docs:** OpenAPI 3 / Swagger
- **Build Tool:** Gradle
- **Static Analysis:** Detekt
- **Code Style & Static Analysis** Ktlint
- **Coverage:** JaCoCo
- **Quality Gate:** SonarQube

---

## API Documentation

The API is fully documented using **OpenAPI 3**.

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
- Docker (for SonarQube)
- `direnv` (recommended)

---

### Configuration & Secrets

Sensitive configuration is **never committed**.

All environment-specific values (database credentials, Redis, OAuth, JWT,
SonarQube, etc.) are provided via **environment variables**.

For local development, the project uses **direnv** with a `.envrc` file
(ignored by Git), ensuring:

- no secrets in the repository
- consistent behavior across CLI, IDE, and CI
- clean separation between code and configuration

#### Install direnv (Linux)

```bash
  sudo apt install direnv
```

Enable it in your shell and allow the project environment:

```bash
  direnv allow
```

---

### Run Locally

Ensure the database schema is created automatically on startup via JPA/Hibernate.

```bash
  ./gradlew bootRun
```

---

## Testing & Quality

This project enforces **strict quality standards**.

- **83% overall test coverage**
- Minimum **80% coverage on new code**
- SonarQube Quality Gate enforced
- Zero known bugs or vulnerabilities

Tests focus primarily on:
- application services
- domain logic
- critical integration paths

The goal is **meaningful coverage**, not artificial metrics.

---

### Code Quality (SonarQube)

Start SonarQube locally:

```bash
  docker compose -f docker/docker-compose.sonar.yml up -d
```

Run the full quality pipeline:

```bash
  ./gradlew test                # Run tests
  ./gradlew detekt              # Static analysis
  ./gradlew jacocoTestReport    # Coverage report
  ./gradlew sonar               # SonarQube analysis
```

- **SonarQube UI:**  
  http://localhost:9001

Stop SonarQube when finished:

```bash
  docker compose -f docker/docker-compose.sonar.yml down
```

---

## Versioning & Milestones

- **v0.1-backend-quality**
    - Backend feature-complete
    - Quality Gate passed
    - Coverage baseline frozen
    - Production-ready testing discipline

Future versions will focus on:
- frontend integration
- performance tuning
- deployment & CI/CD automation

---

## Contributing

Contributions are welcome.

Please follow these guidelines:

- write clean, readable, and secure code
- respect architectural boundaries
- keep public APIs documented
- avoid deprecated APIs
- preserve existing behavior unless explicitly changing it

---

## Why This Project Exists

SynchTask Backend exists to demonstrate **real-world backend engineering**:
not just features, but **quality, structure, and long-term maintainability**.
