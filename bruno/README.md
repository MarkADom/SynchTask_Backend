# Bruno Suite — SynchTask Backend

This directory contains Bruno collections for integration-level API testing of the Spring Boot + Kotlin backend.

## Folder structure

```text
bruno/
  environments/
    local.bru
  integration/
    core-flow/        # Happy path end-to-end actor flow
    security/         # JWT hardening and token validation negatives
    authorization/    # Membership and ownership domain access controls
    validation/       # Request and reference validation negatives
  bruno.json
```

## Suite categories

- **core-flow**: deterministic happy-path flow (register/login actors, create project/board/task, assign, verify access).
- **security**: token-level hardening checks (expired JWT, tampered JWT, invalid audience).
- **authorization**: domain authorization checks (role/membership restrictions and cross-owner isolation).
- **validation**: payload/reference robustness checks (invalid inputs and missing resource references).

## Running the Bruno suite

1. Start the backend locally:
   ```bash
   SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
   ```
2. Open Bruno and load collection:
    - `bruno/bruno.json`
3. Select environment:
    - `bruno/environments/local.bru`
4. Run folders as needed:
    - `integration/core-flow` for happy-path baseline
    - `integration/security` for JWT negative checks
    - `integration/authorization` for access-control negatives
    - `integration/validation` for input/resource validation negatives

## Required environment variables

At minimum, ensure these are defined in the active Bruno environment:

- `baseUrl` (example: `http://localhost:8081`)
- `ownerPassword`
- `memberPassword`
- `outsiderPassword`

For tests that perform direct login for pre-existing users, set:

- `ownerEmail`
- `memberEmail`
- `outsiderEmail`

Most integration negatives in this suite self-register users per run to remain isolated and repeatable.
