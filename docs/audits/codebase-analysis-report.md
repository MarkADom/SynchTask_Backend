# SynchTask Backend – Comprehensive Codebase Analysis

## 1) Invalid `@EntityGraph` usages

### Confirmed invalid

1. **`RefreshTokenRepository.findAllByUserAndIsRevokedFalse`**
   - Declares `@EntityGraph(attributePaths = ["com/synchtask/user"])`.
   - `RefreshToken` entity field is `user`, not `com/synchtask/user`.
   - **Impact:** startup/runtime metadata failure for repository method parsing.
   - **Fix:** change to `@EntityGraph(attributePaths = ["user"])` or remove the annotation (the query already filters by concrete user, and graph here is optional).

2. **`UserRepository` methods** (`findByEmail`, `findByEmailIn`, `findAllByLastActivityAfter`, `findAllByIsOnlineTrue`)
   - Declares graphs with `receivedFriendRequests`, `sentFriendRequests`.
   - `User` entity no longer exposes those relationships.
   - **Impact:** invalid entity graph definitions (references removed/refactored fields).
   - **Fix options:**
     - remove `@EntityGraph` from these methods, or
     - reintroduce explicit mapped relationships in `User` if truly required by read model.

## 2) Repository methods with invalid fetch references

### Invalid fetch references

- **Entity:** `RefreshToken`
  - **Method:** `RefreshTokenRepository.findAllByUserAndIsRevokedFalse`
  - **Invalid relation:** `com/synchtask/user` (missing relationship; correct is `user`).

- **Entity:** `User`
  - **Methods:**
    - `UserRepository.findByEmail`
    - `UserRepository.findByEmailIn`
    - `UserRepository.findAllByLastActivityAfter`
    - `UserRepository.findAllByIsOnlineTrue`
  - **Invalid relations:** `receivedFriendRequests`, `sentFriendRequests` (missing from current `User` model).

### Additional query correctness risk (non-EntityGraph)

- `ChatRoomRepository.findByExactParticipants` uses `:participant MEMBER OF c.participants` but parameter type is `Set<User>`.
- `MEMBER OF` expects a *single element*, not a set.
- Recommended rewrite:
  - Use explicit join + group-by/having count pattern for exact set matching, or
  - iterate candidate room IDs with deterministic intersection logic.

## 3) Domain model inconsistencies

1. **Repository ↔ Entity contract drift**
   - `UserRepository` still assumes friend-request collections on `User`, but the entity is intentionally decoupled.

2. **Owning-side update inconsistency in project-board relation**
   - `Project.boards` is inverse side (`mappedBy = "project"`), while `Board.project` is owning side.
   - In `ProjectService.update`, collection is cleared/added through inverse side only, without setting `board.project = project` for each board.
   - This can lead to silent non-persistence or stale associations.
   - Fix by updating owning side consistently (helper method on aggregate, e.g. `project.assignBoards(...)` that sets `board.project` values).

## 4) User aggregate violations (DDD)

### Current state

- `User` aggregate is relatively clean: no direct object graph to chat/friend/project aggregates.
- Friendship aggregate references only `requesterId` and `friendId` (ID references), which is DDD-friendly.

### Violation found indirectly

- `UserRepository` attempts to fetch friend-request collections from `User`, indicating legacy aggregate coupling in the persistence contract.

### Recommendation

- Keep `User` as identity/profile/auth aggregate only.
- Maintain friend/chat membership as separate aggregates with ID references.
- Build dedicated read models (projections/views) for social graph queries instead of backloading them into `User` entity graphs.

## 5) Security & auth anomalies

### Missing method-level security annotations (relying only on global filter rules)

- `TaskController.createTask` (`POST /tasks`) has no `@PreAuthorize`.
- `TaskController.getTaskDetail` (`GET /tasks/{taskId}`) has no `@PreAuthorize`.
- `TaskResourceController.listLinks` (`GET /tasks/{taskId}/links`) has no `@PreAuthorize`.
- `TaskResourceController.listAttachments` (`GET /tasks/{taskId}/attachments`) has no `@PreAuthorize`.
- `ActivityController.getMyActivities` (`GET /activities`) has no `@PreAuthorize`.

These are currently protected by `anyRequest().authenticated()`, but endpoint intent is less explicit and more fragile to future security config drift.

### Public endpoint review

- `SecurityConfig` permits `/auth/**`; this is broad.
- Sensitive paths under `/auth` are currently guarded via `@PreAuthorize` (e.g., role update and key rotation), so effective access is restricted.
- Recommendation: narrow URL-level permit list to only true public endpoints and keep method-level rules as defense-in-depth.

## 6) Potential N+1 / performance issues

1. **Project listing & mapping**
   - `ProjectService.listAll` maps `ProjectMapper.toResponse`, which accesses `project.members` and `project.boards` (both LAZY).
   - Risk: 1 query for projects + 2N lazy queries.
   - Recommendation: repository method with fetch graph/joins for members+boards when listing, or DTO projection query.

2. **Task page mapping**
   - `TaskMapper.toResponse` dereferences `owner`, `collaborators`, `board`, `board.project`.
   - If task page is fetched without optimized joins, this becomes classic N+1 amplification.
   - Recommendation: projection for task list view or specification with `fetch` joins tuned for list endpoint.

3. **Board response mapping**
   - `BoardMapper.toResponse` accesses `owner.name`.
   - When listing boards with LAZY owner and no fetch plan, triggers per-row user loads.

## 7) Test suite alignment

### High-risk breakage due repository contract drift

- The invalid `@EntityGraph` declarations in `UserRepository` and `RefreshTokenRepository` are likely to break Spring Data repository initialization in context-loading tests.
- Any `@SpringBootTest`, `@DataJpaTest`, or MVC test that wires these repositories can fail early.

### Suggested test updates

1. Add focused repository bootstrap tests:
   - `@DataJpaTest` for `UserRepository` and `RefreshTokenRepository` verifying context starts and methods execute.
2. Add regression tests for project-board assignment:
   - verify `ProjectService.update` persists board ownership changes via owning side (`Board.project`).
3. Add security tests:
   - assert unauthenticated access is denied for `/tasks`, `/activities`, `/tasks/{id}/links`, `/tasks/{id}/attachments`.
   - assert admin-only endpoints under `/auth` remain protected despite `/auth/**` URL-level permit.

## 8) DTO mapping red flags

1. **Cross-aggregate data exposure in list DTOs**
   - `TaskResponseDTO` includes `boardName` and `projectName`; this couples task API view to board/project internals and increases fetch breadth.

2. **Large graph materialization through mappers**
   - `ProjectMapper` exposes `members` emails and board summaries from entity collections directly.
   - For large projects this can be expensive and leak membership data where not needed.

3. **No direct fetch logic in mappers (good)**
   - Mappers themselves do not call repositories or issue fetch joins.
   - The red flag is not fetch-in-mapper logic, but mapper dereferencing lazy relationships without endpoint-specific fetch plans.

## Priority remediation order

1. Fix invalid entity graphs (`RefreshTokenRepository`, `UserRepository`).
2. Correct `ChatRoomRepository.findByExactParticipants` query contract.
3. Enforce owning-side association updates in `ProjectService.update`.
4. Add explicit `@PreAuthorize` on currently implicit protected methods.
5. Introduce projection/fetch-optimized repository methods for heavy list endpoints.
6. Add repository/security regression tests to lock behavior.
