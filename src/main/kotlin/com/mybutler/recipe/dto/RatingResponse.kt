package com.mybutler.recipe.dto

import com.mybutler.recipe.entity.RecipeRating
import org.springframework.data.domain.Page
import java.time.LocalDateTime

data class RatingResponse(
    val id: Long,
    val recipeId: Long,
    val userId: Long,
    val score: Int,
    val comment: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(rating: RecipeRating) = RatingResponse(
            id = rating.id,
            recipeId = rating.recipeId,
            userId = rating.userId,
            score = rating.score,
            comment = rating.comment,
            createdAt = rating.createdAt,
            updatedAt = rating.updatedAt,
        )
    }
}

data class RatingPageResponse(
    val content: List<RatingResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
) {
    companion object {
        fun from(page: Page<RecipeRating>) = RatingPageResponse(
            content = page.content.map(RatingResponse::from),
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            last = page.isLast,
        )
    }
}
