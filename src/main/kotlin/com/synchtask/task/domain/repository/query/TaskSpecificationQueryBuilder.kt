package com.synchtask.task.domain.repository.query

import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskStatus
import com.synchtask.user.domain.entity.User
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * Builds visibility-safe, dynamic task queries.
 *
 * A user can see a task if they:
 * - are a task member
 * - are a board member
 * Uses distinct queries to avoid duplicates caused by joins.
 */
@Component
class TaskSpecificationQueryBuilder(
    private val entityManager: EntityManager,
) {
    fun execute(
        user: User,
        status: TaskStatus?,
        label: String?,
        assigneeId: Long?,
        boardId: Long?,
        pageable: Pageable,
    ): Page<Task> {
        val cb = entityManager.criteriaBuilder

        val query = cb.createQuery(Task::class.java)
        val root = query.from(Task::class.java)

        root.fetch<Task, Any>("owner", JoinType.LEFT)
        root.fetch<Task, Any>("board", JoinType.LEFT)
        root.fetch<Task, Any>("members", JoinType.LEFT)

        val predicates = buildPredicates(cb, root, user, status, label, assigneeId, boardId)
        query.select(root).distinct(true).where(*predicates.toTypedArray())

        val typedQuery = entityManager.createQuery(query)
        typedQuery.firstResult = pageable.offset.toInt()
        typedQuery.maxResults = pageable.pageSize
        val resultList = typedQuery.resultList

        val countQuery = cb.createQuery(Long::class.java)
        val countRoot = countQuery.from(Task::class.java)
        val countPredicates = buildPredicates(cb, countRoot, user, status, label, assigneeId, boardId)
        countQuery.select(cb.countDistinct(countRoot)).where(*countPredicates.toTypedArray())
        val total = entityManager.createQuery(countQuery).singleResult

        // Prevent entity leakage between users due to shared persistence context
        entityManager.clear()

        return PageImpl(resultList, pageable, total)
    }

    private fun buildPredicates(
        cb: CriteriaBuilder,
        root: Root<Task>,
        user: User,
        status: TaskStatus?,
        label: String?,
        assigneeId: Long?,
        boardId: Long?,
    ): MutableList<Predicate> {
        val predicates = mutableListOf<Predicate>()
        val taskMemberJoin = root.join<Task, Any>("members", JoinType.LEFT)
        val boardJoin = root.join<Task, Any>("board", JoinType.LEFT)
        val boardMemberJoin = boardJoin.join<Any, Any>("members", JoinType.LEFT)
        val isTaskMember = cb.equal(taskMemberJoin.get<Any>("user").get<Long>("id"), user.id)
        val isBoardMember = cb.equal(boardMemberJoin.get<Any>("user").get<Long>("id"), user.id)

        predicates.add(cb.or(isTaskMember, isBoardMember))

        status?.let { predicates.add(cb.equal(root.get<TaskStatus>("status"), it)) }

        label?.let {
            val labelsJoin = root.joinSet<Task, String>("labels", JoinType.LEFT)
            predicates.add(cb.equal(labelsJoin, it))
        }

        assigneeId?.let {
            predicates.add(cb.equal(taskMemberJoin.get<Any>("user").get<Long>("id"), it))
        }

        boardId?.let {
            predicates.add(cb.equal(boardJoin.get<Long>("id"), it))
        }

        return predicates
    }
}
