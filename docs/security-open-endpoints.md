# Public vs Protected Endpoints (Test Planning)

This document identifies which endpoints should be treated as public for API tests.

## Source of truth
- URL rules: `SecurityConfig.securityFilterChain()`.
- Method-level restrictions: `@PreAuthorize` on controller methods.

> Important: `permitAll` at URL level does **not** bypass method-level `@PreAuthorize`.

## Effectively public endpoints (recommended for no-auth tests)

### Authentication / discovery
- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `GET /auth/jwks`
- `GET /jwks`
- `GET /auth/.well-known/openid-configuration`

### Docs and infrastructure
- `GET /swagger-ui/**`
- `GET /v3/api-docs/**`
- `GET /actuator/**` (currently public by config, recommended to lock down outside local/dev)

## Endpoints protected despite being under `/auth/*`
- `DELETE /auth/logout` → has `@PreAuthorize("isAuthenticated()")`.
- `PUT /auth/users/{userId}/role` → has `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`.

## OAuth2 nuance
- `/oauth2/**` endpoints are `permitAll` by URL, but user-principal-dependent handlers still require an authenticated OAuth2 session to return meaningful data.

## Endpoints currently protected by default (`anyRequest().authenticated()`)
Examples:
- `/boards/**`
- `/tasks/**`
- `/projects/**`
- `/users/**`
- `/friends/**`
- `/chat/**`
- `/activities`
- `/test/ping`
- `/`

## Testing strategy
1. **Public tests (no token):**
    - Validate success (`2xx`) for effective public endpoints listed above.
2. **Protected tests (no token):**
    - Validate `401 Unauthorized` for protected endpoints.
3. **Role-based tests (with token):**
    - Validate role restrictions (e.g., admin-only endpoints).

## Recommended next hardening step
- Restrict `/actuator/**` to non-production profile or authenticated admin access.
