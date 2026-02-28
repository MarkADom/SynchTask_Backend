# Configuration Guide

This document maps local environment variables to runtime configuration.

## Local environment file

```bash
cp .envrc.example .envrc
```

Main groups:

- **Server**: `SERVER_PORT`
- **Database**: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- **Redis**: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_DATABASE`, `REDIS_TIMEOUT`
- **OAuth2 (Google)**: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`
- **JWT**: `JWT_EXPIRATION`, `JWT_REFRESH_EXPIRATION`, `JWT_ISSUER`, `JWT_JWK_SET_URI`, `JWT_AUDIENCE`, `JWT_ALGORITHM`
- **CORS**: `CORS_ALLOWED_ORIGINS`

## Operational hardening flags

The following properties are used to keep risky/dev-only behavior disabled by default:

- `synchtask.security.allow-dev-rate-limit-bypass` (default: `false`)
    - when `true`, and only under `dev` profile, rate limiting is bypassed.
- `management.endpoints.web.exposure.include` (default: `health,info`)

## Upload constraints

Runtime upload validation is now explicit and configurable:

- `spring.servlet.multipart.max-file-size` (default: `10MB`)
- `spring.servlet.multipart.max-request-size` (default: `12MB`)
- `synchtask.upload.profile.max-size-bytes` (default: `5242880`)
- `synchtask.upload.profile.allowed-content-types` (default: `image/png,image/jpeg,image/webp`)
- `synchtask.upload.attachments.max-size-bytes` (default: `10485760`)
- `synchtask.upload.attachments.allowed-content-types` (default: `application/pdf,image/png,image/jpeg,text/plain`)

## API semantics updates

The API supports normalized mutation contracts with request bodies and more resource-oriented paths. New preferred patterns include:

- `PATCH /tasks/{taskId}/status` with a DTO body
- `POST /tasks/{taskId}/assignees` with a DTO body
- `POST /tasks/{taskId}/links` with a DTO body
- `POST /notifications`
- `PATCH /notifications/{id}/read`
- `PATCH /notifications/users/{userEmail}/read`
- `DELETE /notifications/users/{userEmail}/cache`
- `POST /notifications/maintenance/cleanup`

Legacy notification mutation paths remain temporarily available as backward-compatible aliases.


## Notes

- Never commit `.envrc` or real credentials.
- Default profile is hardened for release (`ddl-auto: validate`, `show-sql: false`, actuator exposure `health,info`).
- `dev` profile keeps local convenience (`ddl-auto: update`, `show-sql: true`, expanded actuator exposure).
- Test profile uses H2 and `create-drop` (`application-test.yml`).
- In production, use migrations (Flyway/Liquibase) instead of auto-DDL.

## Quick verification

After configuring variables:

```bash
./gradlew bootRun
```

Then open:
- `http://localhost:8081/swagger-ui/index.html`
- `http://localhost:8081/v3/api-docs`
