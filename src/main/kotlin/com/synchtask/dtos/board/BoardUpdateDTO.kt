package com.synchtask.dtos.board

data class BoardUpdateDTO(
    val name: String,
    val color: String?,
    val description: String? = null
)
