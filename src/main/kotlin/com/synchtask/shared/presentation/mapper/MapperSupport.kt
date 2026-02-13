package com.synchtask.shared.presentation.mapper

object MapperSupport {

    fun requireId(id: Long?, entityName: String): Long =
        requireNotNull(id) { "$entityName ID cannot be null" }
}
