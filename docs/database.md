# Database

SynchTask uses a relational database (MySQL in local development) with schema
generated via JPA/Hibernate mappings.

## Schema ownership
- Tables are modeled according to aggregate boundaries.
- Join tables enforce uniqueness for (entity_id, user_id) pairs where applicable.

## Constraints & integrity
- Foreign keys are used to enforce relational integrity.
- Unique constraints exist on join tables to prevent duplicated relationships.

## Indexing strategy (high-level)
Indexes are defined to support the most common access patterns:
- Users by email and activity
- Boards by owner and creation date
- Tasks by status and creation date
- Notifications by recipient, read state, and creation date
- Refresh tokens by expiry and revoke state

## Local inspection
Recommended tools:
- DBeaver for ER diagram visualization
- `mysqldump --no-data` for schema export

## ER Diagram
See `docs/er-diagram.png`.
