package com.mybutler.recipe

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.recipe.dto.RatingUpsertRequest
import com.mybutler.recipe.entity.Recipe
import com.mybutler.recipe.entity.RecipeCategory
import com.mybutler.recipe.entity.RecipeRating
import com.mybutler.recipe.repository.RecipeRatingRepository
import com.mybutler.recipe.repository.RecipeRepository
import com.mybutler.recipe.service.RatingService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.cache.CacheManager
import org.springframework.cache.support.NoOpCacheManager
import java.math.BigDecimal
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class RatingServiceTest {

    @Mock lateinit var recipeRepository: RecipeRepository
    @Mock lateinit var ratingRepository: RecipeRatingRepository

    private lateinit var ratingService: RatingService
    private val cacheManager: CacheManager = NoOpCacheManager()

    @BeforeEach
    fun setUp() {
        ratingService = RatingService(recipeRepository, ratingRepository, cacheManager)
    }

    @Test
    fun `upsert - 신규 평점 등록`() {
        val recipe = baseRecipe(id = 1L)
        given(recipeRepository.findById(1L)).willReturn(Optional.of(recipe))
        given(ratingRepository.findByRecipeIdAndUserId(1L, 10L)).willReturn(null)
        val savedRating = rating(id = 1L, recipeId = 1L, userId = 10L, score = 4)
        given(ratingRepository.save(any<RecipeRating>())).willReturn(savedRating)
        given(ratingRepository.countByRecipeId(1L)).willReturn(1L)
        given(ratingRepository.calculateAverageScore(1L)).willReturn(4.0)

        val result = ratingService.upsert(1L, 10L, RatingUpsertRequest(score = 4))

        assertThat(result.score).isEqualTo(4)
        assertThat(recipe.averageRating).isEqualByComparingTo(BigDecimal("4.00"))
        assertThat(recipe.ratingCount).isEqualTo(1)
    }

    @Test
    fun `upsert - 기존 평점 수정`() {
        val recipe = baseRecipe(id = 1L, ratingCount = 1, averageRating = BigDecimal("3.00"))
        given(recipeRepository.findById(1L)).willReturn(Optional.of(recipe))
        val existing = rating(id = 5L, recipeId = 1L, userId = 10L, score = 3)
        given(ratingRepository.findByRecipeIdAndUserId(1L, 10L)).willReturn(existing)
        given(ratingRepository.countByRecipeId(1L)).willReturn(1L)
        given(ratingRepository.calculateAverageScore(1L)).willReturn(5.0)

        val result = ratingService.upsert(1L, 10L, RatingUpsertRequest(score = 5, comment = "Great!"))

        assertThat(result.score).isEqualTo(5)
        assertThat(existing.score).isEqualTo(5)
        assertThat(existing.comment).isEqualTo("Great!")
        verify(ratingRepository, never()).save(any<RecipeRating>())
    }

    @Test
    fun `upsert - 레시피 없을 때 RECIPE_NOT_FOUND`() {
        given(recipeRepository.findById(999L)).willReturn(Optional.empty())

        val ex = assertThrows<BusinessException> {
            ratingService.upsert(999L, 10L, RatingUpsertRequest(score = 4))
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.RECIPE_NOT_FOUND)
    }

    @Test
    fun `getMyRating - 존재하는 평점 반환`() {
        given(recipeRepository.existsById(1L)).willReturn(true)
        given(ratingRepository.findByRecipeIdAndUserId(1L, 10L)).willReturn(rating(recipeId = 1L, userId = 10L, score = 3))

        val result = ratingService.getMyRating(1L, 10L)

        assertThat(result.score).isEqualTo(3)
    }

    @Test
    fun `getMyRating - 평점 없을 때 RECIPE_RATING_NOT_FOUND`() {
        given(recipeRepository.existsById(1L)).willReturn(true)
        given(ratingRepository.findByRecipeIdAndUserId(1L, 10L)).willReturn(null)

        val ex = assertThrows<BusinessException> {
            ratingService.getMyRating(1L, 10L)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.RECIPE_RATING_NOT_FOUND)
    }

    @Test
    fun `deleteMyRating - 평점 삭제 후 평균 재계산`() {
        val recipe = baseRecipe(id = 1L, ratingCount = 2, averageRating = BigDecimal("4.00"))
        given(recipeRepository.findById(1L)).willReturn(Optional.of(recipe))
        given(ratingRepository.deleteByRecipeIdAndUserId(1L, 10L)).willReturn(1L)
        given(ratingRepository.countByRecipeId(1L)).willReturn(1L)
        given(ratingRepository.calculateAverageScore(1L)).willReturn(3.0)

        ratingService.deleteMyRating(1L, 10L)

        assertThat(recipe.ratingCount).isEqualTo(1)
        assertThat(recipe.averageRating).isEqualByComparingTo(BigDecimal("3.00"))
    }

    @Test
    fun `deleteMyRating - 평점 없을 때 RECIPE_RATING_NOT_FOUND`() {
        given(recipeRepository.findById(1L)).willReturn(Optional.of(baseRecipe(id = 1L)))
        given(ratingRepository.deleteByRecipeIdAndUserId(1L, 10L)).willReturn(0L)

        val ex = assertThrows<BusinessException> {
            ratingService.deleteMyRating(1L, 10L)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.RECIPE_RATING_NOT_FOUND)
    }

    @Test
    fun `deleteMyRating - 모든 평점 삭제 시 평균 0으로 초기화`() {
        val recipe = baseRecipe(id = 1L, ratingCount = 1, averageRating = BigDecimal("4.00"))
        given(recipeRepository.findById(1L)).willReturn(Optional.of(recipe))
        given(ratingRepository.deleteByRecipeIdAndUserId(1L, 10L)).willReturn(1L)
        given(ratingRepository.countByRecipeId(1L)).willReturn(0L)

        ratingService.deleteMyRating(1L, 10L)

        assertThat(recipe.ratingCount).isEqualTo(0)
        assertThat(recipe.averageRating).isEqualByComparingTo(BigDecimal.ZERO)
    }

    private fun baseRecipe(
        id: Long = 1L,
        ratingCount: Int = 0,
        averageRating: BigDecimal = BigDecimal.ZERO,
    ) = Recipe(
        id = id,
        name = "Test Recipe",
        category = RecipeCategory.CLASSIC,
        difficulty = 1,
        ratingCount = ratingCount,
        averageRating = averageRating,
        isCustom = false,
    )

    private fun rating(
        id: Long = 1L,
        recipeId: Long = 1L,
        userId: Long = 10L,
        score: Int = 4,
    ) = RecipeRating(id = id, recipeId = recipeId, userId = userId, score = score)
}
