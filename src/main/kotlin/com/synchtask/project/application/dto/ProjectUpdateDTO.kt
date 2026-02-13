package com.synchtask.project.application.dto

import java.time.LocalDate

data class ProjectUpdateDTO(
    val name: String? = null,
    val description: String? = null,
    val tag: String? = null,
    val color: String? = null,
    val dueDate: LocalDate? = null,
    val boardIds: List<Long>? = null
)
