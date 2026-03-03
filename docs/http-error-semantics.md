# HTTP Error Semantics

Canonical error behavior for SynchTask Backend v1.0.

## `400 Bad Request`
Structural validation or malformed input.

Example:
- Missing required DTO field.
- Invalid enum value in request JSON.

## `401 Unauthorized`
Authentication is missing, expired, or invalid.

Example:
- Protected endpoint called without `Authorization: Bearer <token>`.
- Expired JWT.

## `403 Forbidden`
Authenticated caller exists but is not authorized for the action.

Example:
- Authenticated user tries to update a project owned by another user without required role/membership.

## `404 Not Found`
Requested resource does not exist, or is filtered from access paths that return not found.

Example:
- Task ID does not exist.
- Lookup scoped by ownership/membership cannot resolve a visible record.

## `409 Conflict`
Domain conflict prevents state transition.

Example:
- Duplicate friendship request.
- Business rule conflict on state mutation.

## `500 Internal Server Error`
Unexpected runtime error not mapped to a specific domain status.

Example:
- Unhandled exception in service/infrastructure flow.
