package com.mybutler.ar.dto

import java.time.LocalDateTime

data class ArSessionResponse(
    val sessionId: Long,
    val postId: Long,
    val recipeId: Long,
    val rating: Int,
    val caption: String?,
    val photoUrl: String,
    val createdAt: LocalDateTime,
)
