package com.synchtask.task.application.service

import com.synchtask.board.domain.entity.Board
import com.synchtask.board.domain.repository.BoardMemberRepository
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.repository.TaskMemberRepository
import com.synchtask.shared.domain.membership.MembershipRole
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskAuthorizationSupportTest {
    private val taskMemberRepository = mockk<TaskMemberRepository>(relaxed = true)
    private val boardMemberRepository = mockk<BoardMemberRepository>(relaxed = true)

    private fun user(
        id: Long?,
        role: UserRole = UserRole.USER,
    ) =
        User(
            id = id,
            name = "User",
            email = "user@test.com",
            passwordHash = "hash",
            role = role
        )

    private fun task(
        id: Long? = 10L,
        boardId: Long? = 20L,
    ): Task {
        val owner = user(1L)
        val board = Board(
            id = boardId,
            name = "Board",
            owner = owner
        )
        return Task(
            id = id,
            title = "Task",
            description = "Desc",
            owner = owner,
            board = board
        )
    }

    @Test
    fun `should allow admin access`() {
        val result = hasTaskAccess(task(), user(99L, UserRole.ADMIN), taskMemberRepository, boardMemberRepository)

        assertTrue(result)
    }

    @Test
    fun `should deny when actor id is null`() {
        val result = hasTaskAccess(task(), user(null), taskMemberRepository, boardMemberRepository)

        assertFalse(result)
    }

    @Test
    fun `should allow when task member exists`() {
        val actor = user(2L)
        every { taskMemberRepository.existsByTaskIdAndUserId(10L, 2L) } returns true

        val result = hasTaskAccess(task(), actor, taskMemberRepository, boardMemberRepository)

        assertTrue(result)
    }

    @Test
    fun `should allow when board member exists and task member does not`() {
        val actor = user(2L)
        every { taskMemberRepository.existsByTaskIdAndUserId(10L, 2L) } returns false
        every { boardMemberRepository.existsByBoardIdAndUserId(20L, 2L) } returns true

        val result = hasTaskAccess(task(), actor, taskMemberRepository, boardMemberRepository)

        assertTrue(result)
    }

    @Test
    fun `should deny when no memberships exist`() {
        val actor = user(2L)
        every { taskMemberRepository.existsByTaskIdAndUserId(10L, 2L) } returns false
        every { boardMemberRepository.existsByBoardIdAndUserId(20L, 2L) } returns false

        val result = hasTaskAccess(task(), actor, taskMemberRepository, boardMemberRepository)

        assertFalse(result)
    }

    @Test
    fun `should return empty members when task id is null`() {
        val result = resolveTaskMembershipUsers(task(id = null), taskMemberRepository)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should return distinct membership users`() {
        val actor = user(2L)
        val duplicate = user(2L)
        val task = task()
        val memberOne = com.synchtask.task.domain.entity.TaskMember(
            task = task,
            user = actor,
            role = MembershipRole.COLLABORATOR
        )
        val memberTwo = com.synchtask.task.domain.entity.TaskMember(
            task = task,
            user = duplicate,
            role = MembershipRole.COLLABORATOR
        )
        every { taskMemberRepository.findAllByTaskId(10L) } returns listOf(memberOne, memberTwo)

        val result = resolveTaskMembershipUsers(task, taskMemberRepository)

        assertEquals(1, result.size)
    }
}
