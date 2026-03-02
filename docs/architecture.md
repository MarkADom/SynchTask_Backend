# Architecture (Practical Overview)

This is a concise view of how the backend is organized.

## Layers

- **Presentation** (`presentation/controller`): HTTP endpoints and DTO mapping.
- **Application** (`application/service`): use-case orchestration and transactions.
- **Domain** (`domain`): entities, invariants, ownership rules.
- **Infrastructure** (`infrastructure`): persistence, security, integrations.


## Access model (important)

- Authentication uses JWT/OAuth2.
- Most business endpoints require `isAuthenticated()` at controller level.
- Business authorization for boards/projects/tasks is enforced by ownership/membership checks in services (not only by global roles).
- `ROLE_ADMIN` is reserved for admin endpoints (for example user/admin operations and protected actuator endpoints).


## Request flow 

1. Controller validates/parses request.
2. Application service resolves actor (`AuthenticatedUserService`) and coordinates use-case.
3. Domain/service rules enforce ownership or collaborator membership.
4. Repository persists/loads data.
5. Controller returns response DTO.


## Related docs

- Configuration and profiles: [`configuration.md`](configuration.md)
- Public endpoint posture: [`security-open-endpoints.md`](security-open-endpoints.md)
- Aggregate boundaries: [`dataBase/aggregates.md`](dataBase/aggregates.md)
.md`
