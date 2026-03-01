# Bruno Suite — release/v1.0.0-finalize

This directory contains a **clean Bruno collection rebuilt from scratch** for deterministic API validation of the core authorization model.

## How to run

1. Start the backend with a local profile (recommended):
   ```bash
   SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
   ```
2. Open Bruno and select this collection (`bruno/bruno.json`).
3. Select environment: `bruno/environments/local.bru`.
4. Run folder `bruno/release-v1.0.0` sequentially (top-to-bottom by `seq`).

## Execution order

Requests are intentionally ordered and stateful:

1. Register owner
2. Login owner
3. Register member
4. Login member
5. Register outsider
6. Login outsider
7. Owner creates project
8. Owner lists projects
9. Owner creates board
10. Owner links board to project
11. Owner adds member to board
12. Owner creates task
13. Owner assigns task to member
14. Member retrieves task (must succeed)
15. Outsider retrieves task (must be denied)
16. Owner deletes task (optional cleanup)

No hidden cross-folder dependencies are used. All IDs/tokens are captured during the run.

## DB reset instructions (deterministic flow)

Run the suite on a clean DB before validation.

### Option A — recreate local DB/schema
Use your local MySQL reset routine (drop/create schema), then restart the app.

### Option B — if running via Docker
Recreate DB volume/container and restart backend.

After reset, execute the suite from request 1 again.

## Profile instructions

- Use `dev` profile for local runs (`SPRING_PROFILES_ACTIVE=dev`) so schema updates are applied for local iteration.
- Keep `baseUrl` in `bruno/environments/local.bru` aligned with `SERVER_PORT`.

## Actor model

- **Owner**: creates project, board, task; manages board collaborators; assigns task.
- **Member**: regular user added to board membership and task assignees.
- **Outsider**: regular user never added to board/task membership.

## Expected status codes

- `POST /auth/register` → `201`
- `POST /auth/login` → `200`
- `POST /projects` → `201`
- `GET /projects` → `200`
- `POST /boards` → `200`
- `PUT /projects/{id}` (link board) → `200`
- `PUT /boards/{id}/collaborators` → `200`
- `POST /tasks` → `200`
- `PUT /tasks/{id}/assignees` → `200`
- `GET /tasks/{id}` as member → `200`
- `GET /tasks/{id}` as outsider → `403`
- `DELETE /tasks/{id}` (optional) → `200`

## Security semantics

### Membership-driven authorization model
Task access depends on membership:

- direct task membership (task assignee/owner), or
- board membership on the task's board.

The suite validates this by adding only **member** to board/task flows and keeping **outsider** unbound.

### Why outsider is denied (403 vs 404 note)
In this codebase, outsider access to an existing task is rejected as **403 Forbidden** by domain authorization checks. 
In other architectures this can be `404` to hide existence; here the semantic is explicit denial.

### Why token propagation is mandatory
All protected endpoints require a valid bearer JWT. Tokens are captured from `/auth/login` and propagated through 
`Authorization: Bearer {{token}}` so each actor context is tested independently.

## Release integrity coverage

This suite validates release coherence for:

- authentication bootstrap (3 users)
- protected endpoint enforcement
- core project/board/task path
- membership assignment and cross-actor visibility
- negative authorization behavior for non-member access

The flow is deterministic, clean-database friendly, and suitable as a portfolio-ready release smoke contract.
