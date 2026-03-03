# Public vs Protected Endpoints

Source of truth:

- URL rules in `SecurityConfig.securityFilterChain()`
- Method guards via `@PreAuthorize`

## Public endpoints (no token required)

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `GET /auth/jwks`
- `GET /jwks`
- `GET /auth/.well-known/openid-configuration`
- `GET /auth/.well-known/oauth-authorization-server`
- `GET /swagger-ui/**`, `GET /v3/api-docs/**`
- `GET /actuator/health`, `GET /actuator/info`
- `/oauth2/**` entry endpoints

## Protected by authentication

Default is `anyRequest().authenticated()`.
Examples: `/projects/**`, `/boards/**`, `/tasks/**`, `/users/**`, `/friends/**`, `/chat/**`, `/notifications/**`.

Expected behavior in tests:

- Missing/invalid token on protected endpoints => `401 Unauthorized`
- Authenticated but forbidden by domain ownership/membership rules => `403 Forbidden`

## Extra protection notes

- Non-health/info actuator endpoints require `ROLE_ADMIN`.
- `permitAll` at URL level does not bypass stricter method-level `@PreAuthorize` checks.

