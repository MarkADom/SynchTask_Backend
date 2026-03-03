# ER Diagram (Mermaid)

> Source-of-truth snapshot for documentation purposes.
> Keep this file aligned with the current relational schema.

```mermaid
erDiagram
    users ||--o{ refresh_tokens : has
    users ||--o{ user_encryption_keys : owns
    users ||--o{ notifications : receives
    users ||--o{ boards : owns
    users ||--o{ projects : owns

    projects ||--o{ boards : groups
    boards ||--o{ tasks : contains

    tasks ||--o{ task_comments : has
    tasks ||--o{ task_attachments : has
    tasks ||--o{ task_links : has
    tasks ||--o{ task_labels : tagged

    boards ||--o{ board_membership : membership
    tasks ||--o{ task_membership : membership
    projects ||--o{ project_membership : membership

    users ||--o{ friends : requester
    users ||--o{ activities : actor

    chat_rooms ||--o{ chat_room_participants : membership
    chat_rooms ||--o{ chat_messages : contains
    users ||--o{ chat_messages : sender
```

## Notes

- `jwt_keys` is infrastructure persistence and intentionally isolated from business aggregates.
- Join tables (`board_membership`, `project_membership`, `task_membership`, `chat_room_participants`) use composite keys for uniqueness.
