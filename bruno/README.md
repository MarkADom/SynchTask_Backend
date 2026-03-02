# Bruno API Test Suites

This folder contains integration suites for SynchTask backend.

## Structure

```text
bruno/
  environments/
    local.bru
  integration/
    core-flow/
    security/
    authorization/
    validation/
  bruno.json
```

## Independence between suites

Each suite bootstraps its own test users in its own folder (`00-register-*`, `01-login-*`) and can be run independently.
- You **do not** need to run `core-flow` before `security`, `authorization`, or `validation`.
- Run suites separately or all together, depending on what you are validating.

## Environment variables

Required in `environments/local.bru`:

- `baseUrl` (default: `http://localhost:8081`)
- `ownerPassword`
- `memberPassword`
- `outsiderPassword`

Most user emails/tokens are generated dynamically by pre-request scripts.

## Variable strategy

- Shared runtime variable: `runId` (core-flow uses timestamp-based isolation).
- Suite-scoped runtime variables:
    - `securityRunId`, `securityOwnerEmail`, `securityOwnerToken`
    - `authorizationRunId`, `authorizationOwnerEmail`, `authorizationOwnerToken`
    - `validationRunId`, `validationOwnerEmail`, `validationOwnerToken`
- Core-flow runtime variables include `ownerEmail`, `memberEmail`, `outsiderEmail`, plus related tokens and ids.

## Run

1. Start backend:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

2. Open Bruno collection: `bruno/bruno.json`
3. Select environment: `bruno/environments/local.bru`
4. Run the desired folder under `integration/`.
