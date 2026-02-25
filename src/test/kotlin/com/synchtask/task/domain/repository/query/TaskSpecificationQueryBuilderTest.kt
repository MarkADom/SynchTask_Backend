package com.synchtask.task.domain.repository.query

import com.synchtask.board.domain.entity.Board
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskStatus
import com.synchtask.user.domain.entity.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Fetch
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.SetJoin
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import kotlin.test.assertEquals

@Suppress("UNCHECKED_CAST")
class TaskSpecificationQueryBuilderTest {
    private lateinit var entityManager: EntityManager
    private lateinit var criteriaBuilder: CriteriaBuilder
    private lateinit var taskQuery: CriteriaQuery<Task>
    private lateinit var countQuery: CriteriaQuery<Long>
    private lateinit var taskRoot: Root<Task>
    private lateinit var countRoot: Root<Task>
    private lateinit var builder: TaskSpecificationQueryBuilder

    private val actor = User(id = 1L, name = "Actor", email = "actor@test.com", passwordHash = "hash")
    private val board = Board(id = 10L, name = "Board", owner = actor)

    @BeforeEach
    fun setup() {
        entityManager = mockk(relaxed = true)
        criteriaBuilder = mockk(relaxed = true)
        taskQuery = mockk(relaxed = true)
        countQuery = mockk(relaxed = true)
        taskRoot = mockk(relaxed = true)
        countRoot = mockk(relaxed = true)

        builder = TaskSpecificationQueryBuilder(entityManager)

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(Task::class.java) } returns taskQuery
        every { criteriaBuilder.createQuery(Long::class.java) } returns countQuery
        every { taskQuery.from(Task::class.java) } returns taskRoot
        every { countQuery.from(Task::class.java) } returns countRoot

        stubRootPredicates(taskRoot)
        stubRootPredicates(countRoot)

        every { taskQuery.select(taskRoot) } returns taskQuery
        every { taskQuery.distinct(true) } returns taskQuery
        every { taskQuery.where(*anyVararg()) } returns taskQuery

        val countExpr = mockk<Expression<Long>>(relaxed = true)
        every { criteriaBuilder.countDistinct(countRoot) } returns countExpr
        every { countQuery.select(countExpr) } returns countQuery
        every { countQuery.where(*anyVararg()) } returns countQuery
    }

    @Test
    fun `should execute query and clear persistence context with basic filters`() {
        val task =
            Task(
                id = 5L,
                title = "Task",
                description = "Desc",
                owner = actor,
                board = board
            )
        val typedTaskQuery = mockk<TypedQuery<Task>>(relaxed = true)
        val typedCountQuery = mockk<TypedQuery<Long>>(relaxed = true)

        every { entityManager.createQuery(taskQuery) } returns typedTaskQuery
        every { entityManager.createQuery(countQuery) } returns typedCountQuery
        every { typedTaskQuery.resultList } returns listOf(task)
        every { typedCountQuery.singleResult } returns 1L

        val result =
            builder.execute(
                user = actor,
                status = null,
                label = null,
                assigneeId = null,
                boardId = null,
                pageable = PageRequest.of(0, 20)
            )

        assertEquals(1, result.totalElements)
        assertEquals("Task", result.content.first().title)
        verify(exactly = 1) { entityManager.clear() }
    }

    @Test
    fun `should apply optional predicates when all filters are provided`() {
        val labelJoin = mockk<SetJoin<Task, String>>(relaxed = true)
        val taskMemberJoin = mockk<Join<Task, Any>>(relaxed = true)
        val predicate = mockk<Predicate>(relaxed = true)

        every { taskRoot.joinSet<Task, String>("labels", JoinType.LEFT) } returns labelJoin
        every { countRoot.joinSet<Task, String>("labels", JoinType.LEFT) } returns labelJoin

        every { taskRoot.join<Task, Any>("members", JoinType.LEFT) } returns taskMemberJoin
        every { countRoot.join<Task, Any>("members", JoinType.LEFT) } returns taskMemberJoin

        every { criteriaBuilder.equal(labelJoin, "backend") } returns predicate

        val userPath = mockk<Path<Any>>(relaxed = true)
        val userIdPath = mockk<Path<Long>>(relaxed = true)
        every { taskMemberJoin.get<Any>("user") } returns userPath
        every { userPath.get<Long>("id") } returns userIdPath
        every { criteriaBuilder.equal(userIdPath, any<Long>()) } returns predicate

        val typedTaskQuery = mockk<TypedQuery<Task>>(relaxed = true)
        val typedCountQuery = mockk<TypedQuery<Long>>(relaxed = true)
        every { entityManager.createQuery(taskQuery) } returns typedTaskQuery
        every { entityManager.createQuery(countQuery) } returns typedCountQuery
        every { typedTaskQuery.resultList } returns emptyList()
        every { typedCountQuery.singleResult } returns 0L

        val result =
            builder.execute(
                user = actor,
                status = TaskStatus.TODO,
                label = "backend",
                assigneeId = 2L,
                boardId = 10L,
                pageable = PageRequest.of(0, 10)
            )

        assertEquals(0, result.totalElements)
        verify(atLeast = 1) { taskRoot.joinSet<Task, String>("labels", JoinType.LEFT) }
        verify(atLeast = 1) { taskRoot.join<Task, Any>("members", JoinType.LEFT) }
    }

    private fun stubRootPredicates(root: Root<Task>) {
        val collabsPath = mockk<Path<Collection<User>>>(relaxed = true)
        val taskMemberJoin = mockk<Join<Task, Any>>(relaxed = true)
        val taskMemberUserPath = mockk<Path<Any>>(relaxed = true)
        val taskMemberUserIdPath = mockk<Path<Long>>(relaxed = true)

        val boardJoin = mockk<Join<Task, Any>>(relaxed = true)

        val boardMemberJoin = mockk<Join<Any, Any>>(relaxed = true)
        val boardMemberUserPath = mockk<Path<Any>>(relaxed = true)
        val boardMemberUserIdPath = mockk<Path<Long>>(relaxed = true)

        val fetch = mockk<Fetch<Task, Any>>(relaxed = true)
        every { root.fetch<Task, Any>("board", JoinType.LEFT) } returns fetch
        every { root.fetch<Task, Any>("board", JoinType.LEFT) } returns fetch
        every { root.fetch<Task, Any>("members", JoinType.LEFT) } returns fetch

        every { root.join<Task, Any>("members", JoinType.LEFT) } returns taskMemberJoin
        every { taskMemberJoin.get<Any>("user") } returns taskMemberUserPath
        every { taskMemberUserPath.get<Long>("id") } returns taskMemberUserIdPath

        every { root.join<Task, Any>("board", JoinType.LEFT) } returns boardJoin

        every { boardJoin.join<Any, Any>("members", JoinType.LEFT) } returns boardMemberJoin
        every { boardMemberJoin.get<Any>("user") } returns boardMemberUserPath
        every { boardMemberUserPath.get<Long>("id") } returns boardMemberUserIdPath


        val predicate = mockk<Predicate>(relaxed = true)
        every { criteriaBuilder.equal(taskMemberUserIdPath, actor.id) } returns predicate
        every { criteriaBuilder.equal(boardMemberUserIdPath, actor.id) } returns predicate
        every { criteriaBuilder.or(any<Predicate>(), any<Predicate>()) } returns predicate

        val statusPath = mockk<Path<TaskStatus>>(relaxed = true)
        every { root.get<TaskStatus>("status") } returns statusPath
        every { criteriaBuilder.equal(statusPath, any<TaskStatus>()) } returns predicate

        val boardIdPath = mockk<Path<Long>>(relaxed = true)
        every { boardJoin.get<Long>("id") } returns boardIdPath
        every { criteriaBuilder.equal(boardIdPath, any<Long>()) } returns predicate
    }
}
