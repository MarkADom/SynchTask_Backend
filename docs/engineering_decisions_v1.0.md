# Engineering Decisions v1.0

## 1. Architectural Decisions

- The service is intentionally shipped as a monolithic backend for v1.0 to reduce operational overhead and keep delivery scope focused.
- The codebase follows Clean Architecture-inspired layering (presentation, application, domain, infrastructure) with pragmatic coupling where it improves implementation speed and maintainability.
- Packaging is feature-oriented (`board`, `project`, `task`, `security`, etc.) while preserving layer separation inside each feature.

## 2. Authorization Model

- Authorization uses a hybrid model:
    - Global roles (for example admin-only operational actions).
    - Membership and ownership checks at service level for board/project/task operations.
- Ownership and collaborator checks are enforced in application/domain services rather than only via URL-level role checks.
- HTTP semantics are explicit:
    - `401` for missing/invalid authentication.
    - `403` for authenticated users without permission.
    - `404` when resources are not found (including membership-filtered access patterns in some flows).

## 3. Validation Hardening (Sprint 2)

- Structural DTO validation was expanded to enforce request-shape constraints at boundary level.
- `@Valid` is consistently applied on request DTO entry points.
- `IllegalArgumentException` is mapped to `400 Bad Request` through centralized exception handling.
- Result: API contract behavior is stabilized for malformed payloads and invalid argument combinations.

## 4. Security Posture

- JWT signing and verification is configured for `RS256`.
- A JWKS endpoint is exposed for key discovery.
- Key rotation is implemented as a manual, file-based operation in v1.0.
- The API remains stateless at HTTP session level.

## 5. Trade-offs (Conscious Decisions)

- Strict domain isolation from Spring Data abstractions is not fully enforced.
- Key rotation is not automated.
- Concealment normalization between `403` and `404` is not globally standardized.
- Distributed architecture is intentionally deferred; no microservice split in v1.0.
- CI/CD pipeline automation is not included as part of v1.0 scope.

## 6. Future Considerations (Out of v1.0 Scope)

- Improve automated key rotation and operational key management.
- Unify authorization response semantics where concealment vs explicit denial is needed.
- Move closer to strict hexagonal boundaries where practical.
- Expand observability (metrics, tracing, structured operational dashboards).

## v1.0 Closure Checklist

- Authorization contract stabilized
- Validation hardening completed
- Centralized exception mapping implemented
- Membership-based access enforced
- JWT + JWKS configured
- Bruno integration suite green
- Documentation aligned
- Release tag prepared
