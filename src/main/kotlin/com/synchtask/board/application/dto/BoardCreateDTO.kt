package com.synchtask.board.application.dto

data class BoardCreateDTO(
    val name: String,
    val color: String?,
    val description: String? = null
)
