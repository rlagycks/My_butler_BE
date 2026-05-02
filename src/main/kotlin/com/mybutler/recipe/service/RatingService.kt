package com.mybutler.recipe.service

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.recipe.dto.RatingPageResponse
import com.mybutler.recipe.dto.RatingResponse
import com.mybutler.recipe.dto.RatingUpsertRequest
import com.mybutler.recipe.entity.RecipeRating
import com.mybutler.recipe.repository.RecipeRatingRepository
import com.mybutler.recipe.repository.RecipeRepository
import org.springframework.cache.CacheManager
import org.springframework.cache.interceptor.SimpleKey
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
@Transactional(readOnly = true)
class RatingService(
    private val recipeRepository: RecipeRepository,
    private val ratingRepository: RecipeRatingRepository,
    private val cacheManager: CacheManager,
) {
    @Transactional
    fun upsert(recipeId: Long, userId: Long, request: RatingUpsertRequest): RatingResponse {
        val recipe = recipeRepository.findByIdForUpdate(recipeId)
            ?: throw BusinessException(ErrorCode.RECIPE_NOT_FOUND)

        val existing = ratingRepository.findByRecipeIdAndUserId(recipeId, userId)
        val rating = if (existing != null) {
            existing.score = request.score
            existing.comment = request.comment
            existing
        } else {
            ratingRepository.save(RecipeRating(recipeId = recipeId, userId = userId, score = request.score, comment = request.comment))
        }

        recalculate(recipeId, recipe)

        return RatingResponse.from(rating)
    }

    fun getRatings(recipeId: Long, pageable: Pageable): RatingPageResponse {
        if (!recipeRepository.existsById(recipeId)) {
            throw BusinessException(ErrorCode.RECIPE_NOT_FOUND)
        }
        return RatingPageResponse.from(ratingRepository.findByRecipeId(recipeId, pageable))
    }

    fun getMyRating(recipeId: Long, userId: Long): RatingResponse {
        if (!recipeRepository.existsById(recipeId)) {
            throw BusinessException(ErrorCode.RECIPE_NOT_FOUND)
        }
        val rating = ratingRepository.findByRecipeIdAndUserId(recipeId, userId)
            ?: throw BusinessException(ErrorCode.RECIPE_RATING_NOT_FOUND)
        return RatingResponse.from(rating)
    }

    @Transactional
    fun deleteMyRating(recipeId: Long, userId: Long) {
        val recipe = recipeRepository.findByIdForUpdate(recipeId)
            ?: throw BusinessException(ErrorCode.RECIPE_NOT_FOUND)

        val deleted = ratingRepository.deleteByRecipeIdAndUserId(recipeId, userId)
        if (deleted == 0L) {
            throw BusinessException(ErrorCode.RECIPE_RATING_NOT_FOUND)
        }

        recalculate(recipeId, recipe)
    }

    private fun recalculate(recipeId: Long, recipe: com.mybutler.recipe.entity.Recipe) {
        val count = ratingRepository.countByRecipeId(recipeId)
        val average = if (count > 0) {
            BigDecimal.valueOf(ratingRepository.calculateAverageScore(recipeId) ?: 0.0)
                .setScale(2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        recipe.averageRating = average
        recipe.ratingCount = count.toInt()

        if (!recipe.isCustom) {
            cacheManager.getCache("baseRecipes")?.evict(SimpleKey.EMPTY)
        }
    }
}
