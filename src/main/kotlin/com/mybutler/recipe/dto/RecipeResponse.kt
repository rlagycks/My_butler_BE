package com.mybutler.recipe.dto

import com.mybutler.recipe.entity.BaseSpirit
import com.mybutler.recipe.entity.Recipe
import com.mybutler.recipe.entity.RecipeCategory
import com.mybutler.recipe.entity.RecipeIngredient
import com.mybutler.recipe.entity.RecipeStep
import com.mybutler.recipe.entity.TasteTag
import org.springframework.data.domain.Page
import java.math.BigDecimal
import java.time.LocalDateTime

data class RecipeIngredientResponse(
    val id: Long,
    val name: String,
    val amount: String?,
    val unit: String?,
    val displayOrder: Int,
) {
    companion object {
        fun from(ingredient: RecipeIngredient) = RecipeIngredientResponse(
            id = ingredient.id,
            name = ingredient.name,
            amount = ingredient.amount,
            unit = ingredient.unit,
            displayOrder = ingredient.displayOrder,
        )
    }
}

data class RecipeStepResponse(
    val id: Long,
    val stepOrder: Int,
    val description: String,
) {
    companion object {
        fun from(step: RecipeStep) = RecipeStepResponse(
            id = step.id,
            stepOrder = step.stepOrder,
            description = step.description,
        )
    }
}

data class RecipeSummaryResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val thumbnailUrl: String?,
    val category: RecipeCategory,
    val baseSpirit: BaseSpirit?,
    val difficulty: Int,
    val estimatedMinutes: Int?,
    val abv: BigDecimal?,
    val averageRating: BigDecimal,
    val ratingCount: Int,
    val tasteTags: Set<TasteTag>,
    val isCustom: Boolean,
) {
    companion object {
        fun from(recipe: Recipe) = RecipeSummaryResponse(
            id = recipe.id,
            name = recipe.name,
            description = recipe.description,
            thumbnailUrl = recipe.thumbnailUrl,
            category = recipe.category,
            baseSpirit = recipe.baseSpirit,
            difficulty = recipe.difficulty,
            estimatedMinutes = recipe.estimatedMinutes,
            abv = recipe.abv,
            averageRating = recipe.averageRating,
            ratingCount = recipe.ratingCount,
            tasteTags = recipe.tasteTags.toSet(),
            isCustom = recipe.isCustom,
        )
    }
}

data class RecipeDetailResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val thumbnailUrl: String?,
    val category: RecipeCategory,
    val baseSpirit: BaseSpirit?,
    val difficulty: Int,
    val estimatedMinutes: Int?,
    val abv: BigDecimal?,
    val averageRating: BigDecimal,
    val ratingCount: Int,
    val tasteTags: Set<TasteTag>,
    val isCustom: Boolean,
    val authorId: Long?,
    val ingredients: List<RecipeIngredientResponse>,
    val steps: List<RecipeStepResponse>,
    val communityPhotos: List<String>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(recipe: Recipe) = RecipeDetailResponse(
            id = recipe.id,
            name = recipe.name,
            description = recipe.description,
            thumbnailUrl = recipe.thumbnailUrl,
            category = recipe.category,
            baseSpirit = recipe.baseSpirit,
            difficulty = recipe.difficulty,
            estimatedMinutes = recipe.estimatedMinutes,
            abv = recipe.abv,
            averageRating = recipe.averageRating,
            ratingCount = recipe.ratingCount,
            tasteTags = recipe.tasteTags.toSet(),
            isCustom = recipe.isCustom,
            authorId = recipe.authorId,
            ingredients = recipe.ingredients.map(RecipeIngredientResponse::from),
            steps = recipe.steps.map(RecipeStepResponse::from),
            communityPhotos = emptyList(),
            createdAt = recipe.createdAt,
            updatedAt = recipe.updatedAt,
        )
    }
}

data class RecipePageResponse(
    val content: List<RecipeSummaryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
) {
    companion object {
        fun from(pageData: Page<Recipe>) = RecipePageResponse(
            content = pageData.content.map(RecipeSummaryResponse::from),
            page = pageData.number,
            size = pageData.size,
            totalElements = pageData.totalElements,
            totalPages = pageData.totalPages,
            last = pageData.isLast,
        )
    }
}

data class RecipeHomeResponse(
    val availableRecipes: List<RecipeSummaryResponse>,
    val nearlyAvailableRecipes: List<RecipeSummaryResponse>,
    val preferenceRecommendations: List<RecipeSummaryResponse>,
)

data class RecipeRecommendationResponse(
    val availableRecipes: List<RecipeSummaryResponse>,
    val nearlyAvailableRecipes: List<RecipeSummaryResponse>,
)
