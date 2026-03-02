# Documentation Audit Report (v1.0)

## Inventory (what exists)

- `README.md`: project entry point, local run, API docs, tests.
- `SECURITY.md`: vulnerability reporting policy.
- `docs/README.md`: docs index.
- `docs/architecture.md`: layer boundaries and access model.
- `docs/configuration.md`: profile behavior and environment variables.
- `docs/security-open-endpoints.md`: public/protected endpoint policy.
- `docs/dataBase/*`: schema, aggregate boundaries, ER diagram.
- `docs/openapi/api-docs.json`: OpenAPI export.
- `bruno/README.md`: suite structure, variables, run instructions.

## Redundant / outdated before this audit

- Overlap between root README and docs pages (setup and testing repeated in multiple places).
- Outdated statement saying actuator endpoints are public by default.
- Wrong/misaligned references in docs index (`docs/bruno/...` path did not exist).
- Configuration page contained non-essential API semantics details that belong to API docs.

## Missing (only essential)

- A short audit/consolidation record for v1.0 release docs posture (this file).
- Explicit Bruno suite independence and variable naming guidance in one place.

## Minimal v1.0 docs set (kept)

1. **Root README**: quick start + docs map.
2. **SECURITY.md**: reporting policy.
3. **docs/**
    - `README.md` index
    - `architecture.md`
    - `configuration.md`
    - `security-open-endpoints.md`
    - `dataBase/*` (kept because still useful for domain + DB review)
    - `openapi/api-docs.json`
4. **bruno/README.md**: test suite execution and variables.

## Alignment checks done against code/tests

- Security URL and method guard behavior verified from `SecurityConfig` and controller `@PreAuthorize` usage.
- Profile and env-var docs checked against `application.yml`, `application-dev.yml`, `application-test.yml`, and `.envrc.example`.
- Bruno suite behavior and variable strategy checked against files under `bruno/integration/*` and `bruno/environments/local.bru`.
