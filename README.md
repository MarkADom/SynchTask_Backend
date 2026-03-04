# SynchTask Backend

![Release](https://img.shields.io/github/v/release/MarkADom/SynchTask_Backend?style=for-the-badge)
![Status](https://img.shields.io/badge/status-stable-blue?style=for-the-badge)
![Build](https://img.shields.io/badge/build-passing-2ea44f?style=for-the-badge)
![Coverage](https://img.shields.io/badge/coverage-80%25-2ea44f?style=for-the-badge)
![Quality](https://img.shields.io/badge/quality-passed-2ea44f?style=for-the-badge)

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.x-7F52FF?style=for-the-badge)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-4CAF50?style=for-the-badge)
![Gradle](https://img.shields.io/badge/Gradle-8.x-02303A?style=for-the-badge)


SynchTask Backend is a collaborative task management API built with Kotlin (JVM) and Spring Boot 3.

It serves as the backend core of the SynchTask ecosystem and is developed as engineering-focused backend project

> Current stable release: **v1.0.0**
> 
> This version establishes a contract-stable and security-hardened backend baseline.

## Scope (v1.0)

- Monolithic backend (no microservices)
- Stateless JWT security (RS256 + JWKS)
- Hybrid authorization (global roles + membership-based access)
- Validation-hardened HTTP contract
- Backend-only repository (frontend under development)

## Trade-offs & Design Decisions

See: [Engineering Decisions (v1.0)](docs/engineering_decisions_v1.0.md)

## Tech stack

- Kotlin + Spring Boot 3
- MySQL + Redis
- OAuth2 Resource Server with RS256 JWT
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

2. Run the application:

```bash
./gradlew bootRun
```

3. Run with dev profile:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

4. Open API docs:

- Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

## Testing & quality

```bash
./gradlew test
./gradlew detekt
./gradlew jacocoTestReport
```

## Documentation map

- [Architecture](docs/architecture.md)
- [Configuration](docs/configuration.md)
- [HTTP Error Semantics](docs/http-error-semantics.md)
- [Database](docs/database/)
- [Engineering Decisions (v1.0)](docs/engineering_decisions_v1.0.md)
- [OpenAPI Export](docs/openapi/api-docs.json)
- [Bruno Test Suites](bruno/README.md)


## Roadmap

### v1.1 (Planned)

- Pagination & sorting standardization 
- Observability improvements (structured logging + metrics exposure)
- Membership query performance optimization 
- OpenAPI schema enrichment (error models + examples)
- Extended integration tests for authorization edge cases 

### Longer-term 
- Event-driven extensions 
- Modularization of bounded contexts 
- Public demo deployment
## License

Licensed under the terms in [`LICENSE.md`](LICENSE.md).
