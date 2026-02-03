package com.synchtask.board.application.dto

data class BoardUpdateDTO(
    val name: String,
    val color: String?,
    val description: String? = null
)
