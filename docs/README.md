# Documentation Index

This folder contains the technical documentation for SynchTask Backend.

## API testing

- `bruno/` — Bruno collections and local environment template.
- `bruno/security-open-endpoints.md` — public vs protected endpoint notes used for test planning.

## Database and domain model

- `dataBase/database.md` — current relational schema, constraints, indexes, and operational notes.
- `dataBase/aggregates.md` — aggregate boundaries (DDD) and ownership rules.
- `dataBase/er-diagram.md` — textual ER representation (Mermaid) to keep model documentation versioned.

## Recommended maintenance flow

When schema/domain changes:

1. Update entities/migrations.
2. Update `dataBase/database.md` and `dataBase/aggregates.md`.
3. Update `dataBase/er-diagram.md`.
4. Update relevant Bruno tests if endpoint contracts changed.
