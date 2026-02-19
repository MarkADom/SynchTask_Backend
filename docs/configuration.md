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
- **JWT**: `JWT_EXPIRATION`, `JWT_REFRESH_EXPIRATION`, `JWT_ISSUER`, `JWT_AUDIENCE`, `JWT_ALGORITHM`
- **CORS**: `CORS_ALLOWED_ORIGINS`

## Notes

- Never commit `.envrc` or real credentials
- For local development, JPA uses `ddl-auto: update`
- Test profile uses H2 and `create-drop` (`application-test.yml`)
- In production, use migrations (Flyway/Liquibase) instead of auto-DDL

## Quick verification

After configuring variables:

```bash
./gradlew bootRun
```

Then open:
- `http://localhost:8081/swagger-ui/index.html`
- `http://localhost:8081/v3/api-docs`
