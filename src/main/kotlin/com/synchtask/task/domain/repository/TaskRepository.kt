package com.synchtask.task.domain.repository

import com.synchtask.task.domain.entity.Task
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskRepository : JpaRepository<Task, Long>

// TODO: If in the future create domain-specific methods (e.g. existsByBoardAndId) here
