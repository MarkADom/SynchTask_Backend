# Architecture (Practical Overview)

This is a concise view of how the backend is organized.

## Layers

- **API / Controllers**: input validation, request/response mapping, auth entry points
- **Application layer**: orchestrates use-cases and transactions
- **Domain layer**: business rules and aggregate boundaries
- **Infrastructure layer**: persistence, security adapters, external integrations

## Design principles

- Keep business logic outside framework-specific code
- Respect aggregate boundaries (User, Board, Project, ChatRoom)
- Prefer explicit use-cases over “smart controllers”
- Keep repository access aggregate-scoped

## Request flow (high-level)

1. Request reaches controller
2. Controller delegates to application service/use-case
3. Domain rules are applied
4. Repositories persist/fetch data
5. Response DTO is returned

## Related docs

- Aggregate boundaries: `docs/dataBase/aggregates.md`
- Schema notes: `docs/dataBase/database.md`
- Endpoint auth test planning: `docs/bruno/security-open-endpoints.md`
