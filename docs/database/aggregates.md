# Aggregate Boundaries (DDD)

This document reflects the current relational schema (`schema.sql` / MySQL dump)
and describes aggregate boundaries used in SynchTask.

## User Aggregate
**Aggregate Root:** `User`

**Primary table:**
- `users`

**Owned entities / tables:**
- `refresh_tokens`
- `user_encryption_keys`
- `notifications`
- `friends`
- `activities` (actor/audit trail)

**Responsibilities:**
- Identity lifecycle and account state (`is_active`, `is_online`, `last_login`)
- Token lifecycle (refresh issue/revoke)
- Personal cryptographic key publication
- Friendship state machine (`PENDING`, `ACCEPTED`, `REJECTED`, `BLOCKED`)
- Notification inbox and read/delivery status
- User activity tracking for domain actions

**Notes:**
- `jwt_keys` is infrastructure/security persistence and is intentionally outside
  a user-owned aggregate table (no `user_id` foreign key in schema).
- Cross-aggregate reads are allowed at application service level; repository
  access should remain aggregate-scoped.

---

## Board Aggregate
**Aggregate Root:** `Board`

**Primary table:**
- `boards`

**Owned entities / tables:**
- `tasks`
- `task_comments`
- `task_attachments`
- `task_links`
- `task_labels`
- `board_membership`
- `task_membership`

**Responsibilities:**
- Board ownership and collaboration management
- Task lifecycle and status transitions within board scope
- Task metadata (labels, links, attachments, comments)
- Membership visibility through board/task membership join tables


**Rules:**
- Tasks are modeled as board-contained entities, not standalone aggregate roots.
- Task mutations should be coordinated by board-level use-cases.

---

## Project Aggregate
**Aggregate Root:** `Project`

**Primary table:**
- `projects`

**Owned entities / tables:**
- `project_membership`

**References:**
- `boards.project_id` links boards to projects

**Responsibilities:**
- Project ownership and membership
- Grouping/organization of boards
- Planning metadata (`due_date`, `tag`, `color`)

**Notes:**
- Project references boards, but board-owned entities (`tasks`, comments,
  attachments, links, labels) remain inside the Board aggregate boundary.

---

## Chat Aggregate
**Aggregate Root:** `ChatRoom`

**Primary table:**
- `chat_rooms`

**Owned entities / tables:**
- `chat_room_participants`
- `chat_messages`

**Responsibilities:**
- Room lifecycle
- Participant membership
- Message persistence (`encrypted_message`, timestamp ordering)

**Notes:**
- Chat remains isolated from Board/Task mutation workflows.

---

## Security Infrastructure Persistence

These tables support security infrastructure concerns and are intentionally
outside business aggregates:

- `jwt_keys`

This separation keeps domain aggregates focused while still documenting all
schema artifacts.
