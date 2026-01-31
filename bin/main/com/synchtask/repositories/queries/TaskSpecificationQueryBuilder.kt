package com.synchtask.repositories.queries

import com.synchtask.entities.Task
import com.synchtask.entities.TaskStatus
import com.synchtask.entities.User
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class TaskSpecificationQueryBuilder(
    private val entityManager: EntityManager
) {

    fun execute(
        user: User,
        status: TaskStatus?,
        label: String?,
        assigneeId: Long?,
        boardId: Long?,
        pageable: Pageable
    ): Page<Task> {
        val cb = entityManager.criteriaBuilder

        // Main query
        val query = cb.createQuery(Task::class.java)
        val root = query.from(Task::class.java)
        root.fetch<Task, Any>("collaborators", JoinType.LEFT)
        root.fetch<Task, Any>("comments", JoinType.LEFT)

        val predicates = buildPredicates(cb, root, user, status, label, assigneeId, boardId)
        query.select(root).distinct(true).where(*predicates.toTypedArray())

        val typedQuery = entityManager.createQuery(query)
        typedQuery.firstResult = pageable.offset.toInt()
        typedQuery.maxResults = pageable.pageSize
        val resultList = typedQuery.resultList

        // Count query
        val countQuery = cb.createQuery(Long::class.java)
        val countRoot = countQuery.from(Task::class.java)
        val countPredicates = buildPredicates(cb, countRoot, user, status, label, assigneeId, boardId)
        countQuery.select(cb.countDistinct(countRoot)).where(*countPredicates.toTypedArray())
        val total = entityManager.createQuery(countQuery).singleResult

        return PageImpl(resultList, pageable, total)
    }

    private fun buildPredicates(
        cb: jakarta.persistence.criteria.CriteriaBuilder,
        root: jakarta.persistence.criteria.Root<Task>,
        user: User,
        status: TaskStatus?,
        label: String?,
        assigneeId: Long?,
        boardId: Long?
    ): MutableList<Predicate> {
        val predicates = mutableListOf<Predicate>()
        val isOwner = cb.equal(root.get<Any>("owner"), user)
        val isCollaborator = cb.isMember(user, root.get("collaborators"))
        predicates.add(cb.or(isOwner, isCollaborator))

        status?.let { predicates.add(cb.equal(root.get<TaskStatus>("status"), it)) }
        label?.let {
            val labelsJoin = root.join<Task, String>("labels")
            predicates.add(cb.equal(labelsJoin, it))
        }
        assigneeId?.let {
            val collabJoin = root.join<Task, User>("collaborators")
            predicates.add(cb.equal(collabJoin.get<Long>("id"), it))
        }
        boardId?.let {
            predicates.add(cb.equal(root.get<Any>("board").get<Long>("id"), it))
        }

        return predicates
    }
}
