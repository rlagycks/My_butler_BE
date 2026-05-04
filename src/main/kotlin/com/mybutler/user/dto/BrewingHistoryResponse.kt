package com.mybutler.user.dto

import com.mybutler.ar.entity.ArSession
import java.time.LocalDateTime

data class BrewingHistoryEntry(
    val sessionId: Long,
    val recipeId: Long,
    val recipeName: String,
    val rating: Int,
    val caption: String?,
    val photoUrl: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(session: ArSession, recipeName: String) = BrewingHistoryEntry(
            sessionId = session.id,
            recipeId = session.recipeId,
            recipeName = recipeName,
            rating = session.rating.toInt(),
            caption = session.caption,
            photoUrl = session.photoUrl,
            createdAt = session.createdAt,
        )
    }
}

data class BrewingHistoryResponse(
    val content: List<BrewingHistoryEntry>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
)
