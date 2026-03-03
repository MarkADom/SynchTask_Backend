# Public vs Protected Endpoints

Source of truth: `SecurityConfig.securityFilterChain()`.

## Public endpoints (`permitAll`)

### Authentication and discovery

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `GET /auth/jwks`
- `GET /jwks`
- `GET /auth/.well-known/openid-configuration`
- `/oauth2/**`

### API documentation and static helpers

- `/swagger-ui/**`
- `/swagger-ui.html`
- `/v3/api-docs`
- `/v3/api-docs/**`
- `/swagger-resources/**`
- `/webjars/**`
- `/favicon.ico`

### WebSocket handshake endpoints

- `/ws`
- `/ws/**`
- `/ws-notifications`
- `/ws-notifications/**`

Notes:
- These are HTTP handshake paths allowed at URL filter level.
- Message-level authorization still applies in WebSocket security configuration.

### Other public routes

- `OPTIONS /**` (CORS preflight)
- `GET /actuator/health`
- `GET /actuator/info`
- `/error`

## Protected endpoints

- Default rule: `anyRequest().authenticated()`.
- Non-health/info actuator endpoints: `ROLE_ADMIN` required.
- Domain endpoints (`/projects/**`, `/boards/**`, `/tasks/**`, `/users/**`, `/friends/**`, `/chat/**`, `/notifications/**`) require authenticated access, with additional service-level ownership/membership checks where applicable.

## Expected security semantics

- Missing/invalid token on protected endpoint: `401 Unauthorized`
- Authenticated but denied by role/ownership/membership rules: `403 Forbidden`
- Resource cannot be resolved for caller scope in some service flows: `404 Not Found`
