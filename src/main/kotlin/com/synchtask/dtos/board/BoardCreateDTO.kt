package com.synchtask.dtos.board

data class BoardCreateDTO(
    val name: String,
    val color: String?,
    val description: String? = null
)
