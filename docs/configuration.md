# Configuration Guide

## Profiles

- **Default (`application.yml`)**: release posture (`ddl-auto=validate`, reduced actuator exposure, quieter logs).
- **Dev (`application-dev.yml`)**: local convenience (`ddl-auto=update`, verbose logs, broader actuator exposure).
- **Test (`application-test.yml`)**: H2 in-memory, `create-drop`, WebSocket disabled.

Run locally with dev profile:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

## Required environment variables

Start from template:

```bash
cp .envrc.example .envrc
```

Main variables used by runtime config:

- Server: `SERVER_PORT`
- Database: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- Redis: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_DATABASE`, `REDIS_TIMEOUT`
- OAuth2: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`
- JWT: `JWT_EXPIRATION`, `JWT_REFRESH_EXPIRATION`, `JWT_ISSUER`, `JWT_JWK_SET_URI`, `JWT_AUDIENCE`, `JWT_ALGORITHM`
- CORS: `CORS_ALLOWED_ORIGINS`
- Swagger OAuth helper: `SWAGGER_OAUTH_CLIENT_SECRET`

## Notes

- Never commit `.envrc` or real credentials.
- Release environments should keep schema migrations external (Flyway/Liquibase) and avoid auto-DDL updates.
