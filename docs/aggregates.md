# Aggregate Boundaries (DDD)

This document describes the main aggregates in SynchTask and the responsibilities
and consistency boundaries enforced by each Aggregate Root.

## User Aggregate
**Aggregate Root:** `User`

**Owned entities / tables:**
- `refresh_tokens`
- `user_encryption_keys`
- `jwt_keys` (infrastructure-backed persistence of signing keys)
- `friends`
- `notifications`

**Responsibilities:**
- Identity and authentication lifecycle
- Token management (issue / revoke)
- User encryption key lifecycle
- Social relationships and notifications

**Notes:**
- Cross-aggregate reads are allowed at the service layer, but repositories must
  remain aggregate-scoped.

---

## Board Aggregate
**Aggregate Root:** `Board`

**Owned entities / tables:**
- `tasks`
- `task_comments`
- `task_attachments`
- `task_links`
- `task_labels`
- `board_collaborators`
- `task_collaborators`

**Responsibilities:**
- Board ownership and access control
- Task lifecycle within the board boundary
- Collaboration rules within the board

**Rules:**
- Tasks are not treated as a standalone aggregate root.
- Task mutations should be performed through the `Board` boundary (application layer).

---

## Project Aggregate
**Aggregate Root:** `Project`

**Owned entities / tables:**
- `projects`
- `project_members`

**Responsibilities:**
- Grouping boards under a project
- Project membership and ownership
- Organizational structure (not task ownership)

**Notes:**
- A project may reference boards, but does not directly own tasks.

---

## Chat Aggregate
**Aggregate Root:** `ChatRoom`

**Owned entities / tables:**
- `chat_rooms`
- `chat_room_participants`
- `chat_messages`

**Responsibilities:**
- Room and participant management
- Message persistence and retrieval
- Domain isolation from Board/Task contexts
