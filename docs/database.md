# Database

SynchTask uses a relational database (MySQL in local development), with schema
currently generated via JPA/Hibernate mappings.

## Current schema snapshot

The current MySQL dump includes **20 tables**:

- `activities`
- `board_collaborators`
- `boards`
- `chat_messages`
- `chat_room_participants`
- `chat_rooms`
- `friends`
- `jwt_keys`
- `notifications`
- `project_members`
- `projects`
- `refresh_tokens`
- `task_attachments`
- `task_collaborators`
- `task_comments`
- `task_labels`
- `task_links`
- `tasks`
- `user_encryption_keys`
- `users`

## Relational integrity

Foreign keys enforce the most important consistency rules, including:

- `boards.owner_id -> users.id`
- `boards.project_id -> projects.id`
- `tasks.board_id -> boards.id`
- `tasks.owner_id -> users.id`
- `notifications.recipient_id -> users.id`
- `refresh_tokens.user_id -> users.id`
- `chat_messages.chat_room_id -> chat_rooms.id`
- `chat_messages.sender_id -> users.id`

Join tables with composite primary keys guarantee uniqueness of memberships:

- `board_collaborators (board_id, user_id)`
- `project_members (project_id, user_id)`
- `chat_room_participants (chat_room_id, user_id)`
- `task_collaborators (task_id, user_id)`
- `task_labels (task_id, label)`

## Key uniqueness constraints

- `users.email` unique (`idx_user_email`)
- `refresh_tokens.token` unique
- `friends (requester_id, friend_id)` unique
- `user_encryption_keys.user_id` unique

## Enum domains present in schema

- `tasks.priority`: `LOW`, `MID`, `HIGH`
- `tasks.status`: `TODO`, `IN_PROGRESS`, `REVIEW`, `BLOCKED`, `COMPLETED`
- `friends.status`: `PENDING`, `ACCEPTED`, `REJECTED`, `BLOCKED`
- `notifications.type`: `PERSONAL`, `SYSTEM`, `TASK_UPDATE`, `FRIEND_REQUEST`, `GROUP`, `INVITATION`, `CHAT_MESSAGE`
- `users.role`: `USER`, `COLLABORATOR`, `OWNER`, `ADMIN`
- `activities.type`: board/project/task activity event types

## Indexing highlights

Indexes are present for common query patterns, for example:

- timeline queries by creation date:
    - `idx_activity_created_at`, `idx_board_created_at`, `idx_project_created_at`, `idx_task_created_at`
- actor/recipient filters:
    - `idx_activity_actor`, `idx_notification_recipient_created`, `idx_notification_recipient_read`
- task and friendship filtering:
    - `idx_task_status`, `idx_friend_status`, `idx_friend_requester_status`, `idx_friend_friend_status`
- token maintenance:
    - `idx_refresh_expiry`, `idx_refresh_revoked`, `idx_refresh_user_revoked`

## Operational notes for release

- Current app config still uses `ddl-auto: create-drop` for local development,
  which is fine for dev/test but should not be used in persistent production.
- Keep the MySQL dump and ER diagram synchronized with each release candidate.

## ER Diagram

See `docs/er-diagram.png`.
