
# SynchTask API Test Flows (Bruno)

This directory contains structured API test flows for the SynchTask backend.

The goal is to validate:

- Authentication
- Project / Board / Task lifecycle
- Collaboration features
- Social features
- Administrative operations

The test flows are separated by responsibility to avoid role conflicts and JWT invalidation issues.

---

# Folder Structure
```
    bruno/
    │
    ├── environments/
    │   └── local.bru
    │
    ├── core/
    │   ├── 01-register-demo-user.bru
    │   ├── 02-login-demo-user.bru
    │   ├── 03-create-project.bru
    │   ├── 04-create-board.bru
    │   ├── 05-create-task.bru
    │   ├── 06-update-task.bru
    │   └── 07-delete-task.bru
    │
    ├── collaboration/
    │   ├── 01-add-board-collaborator.bru
    │   ├── 02-assign-task.bru
    │   └── 03-send-notification.bru
    │
    ├── social/
    │   ├── 01-send-friend-request.bru
    │   ├── 02-accept-friend-request.bru
    │   ├── 03-create-chat-room.bru
    │   ├── 04-get-chat-room-messages.bru
    │   └── 05-send-chat-message.bru
    │
    └── admin/
        ├── 01-login-admin.bru
        ├── 02-clear-cache.bru
        └── 03-rotate-keys.bru
````
---

# Backend Setup

Start the backend locally:

```bash
  ./gradlew bootRun
```
Expected base URL:

http://localhost:8081

---

# Database Requirements

You must have:

- Database created (synchtask)
- Schema applied
- An ADMIN user inserted manually

Example:
```mysql
        INSERT INTO users (
          name,
          email,
          password_hash,
          role,
          is_active,
          is_online,
          onboarding_notified,
          created_at
        )
        VALUES (
          'Admin',
          'admin@example.com',
          '<BCryptHash>', 
          'ADMIN',
          b'1',
          b'0',
          b'0',
          NOW()
        );
```
---

# Environment Configuration

Open:

bruno/environments/local.bru
    
Ensure:

    vars {
        baseUrl: http://localhost:8081
        adminEmail: admin@example.com
        adminPassword: AdminPass123!
    }

Tokens are generated dynamically. Do NOT store tokens manually.

---

# Running the Tests

## Core Flow (Main Lifecycle)

Run folder:

core/

Executes:

1. Register Demo User
2. Login Demo User
3. Create Project
4. Create Board
5. Create Task
6. Update Task
7. Delete Task

All requests use {{userAccessToken}}.

---

## Collaboration Flow

Run after core/:

collaboration/

Requires valid userAccessToken, boardId, and taskId.

---

## Social Flow

Run after core/:

social/

Requires valid userAccessToken and stored chatRoomId.

---

## Admin Flow (Run Separately)

Run folder:

admin/

Contains:

1. Login Admin
2. Clear Cache
3. Rotate Keys

IMPORTANT:
Do NOT run rotate-keys before core tests.
It invalidates existing JWT tokens.

---

# Recommended Execution Order

1. core/
2. collaboration/
3. social/
4. admin/ (optional, isolated)

Never mix admin and core flows in the same runner.

---

# Common Errors & Fixes

401 Unauthorized:
- Wrong token variable
- Using admin token for OWNER endpoints
- Running rotate-keys before core flow

500 Internal Server Error:
- Backend exception
- Invalid role value in database
- Missing required DB column

Undefined Variables:
- projectId not stored (previous request failed)
- boardId not stored
- taskId not stored

Fix the first failing request.
Subsequent failures are cascading errors.

---

# Design Principles

- Role-based separation (USER vs ADMIN)
- Clean execution order
- Stateless JWT validation
- No token persistence between runs
- Separation of functional and administrative concerns

This structure reflects production-grade API flow validation.
