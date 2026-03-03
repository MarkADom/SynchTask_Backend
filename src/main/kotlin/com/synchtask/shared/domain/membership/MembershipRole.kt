package com.synchtask.shared.domain.membership

/**
 * Contextual membership role assigned within a specific aggregate boundary.
 *
 * Unlike global platform roles, this role is scoped to a concrete aggregate instance
 * such as a Project, Board, or Task.
 */
enum class MembershipRole {
    OWNER,
    COLLABORATOR
}
