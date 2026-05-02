package com.mybutler.recipe

import com.mybutler.recipe.entity.Recipe
import com.mybutler.recipe.entity.RecipeCategory
import com.mybutler.recipe.entity.TasteTag
import com.mybutler.recipe.service.RecipeRecommendationService
import com.mybutler.user.entity.PreferredAbv
import com.mybutler.user.entity.TastePreference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class RecipeRecommendationServiceTest {

    @Test
    fun `recommend - 취향 없으면 전체 레시피 limit 개 반환`() {
        val recipes = (1..15L).map { recipe(id = it) }

        val result = RecipeRecommendationService.recommend(
            recipes = recipes,
            tasteTags = emptySet(),
            preferredAbv = null,
            limit = 10,
        )

        assertThat(result).hasSize(10)
    }

    @Test
    fun `recommend - taste 매칭 레시피만 포함하고 점수 높은 순 정렬`() {
        val sweetBitter = recipe(id = 1L, tasteTags = setOf(TasteTag.SWEET, TasteTag.BITTER), abv = BigDecimal("20.0"))
        val sweetOnly = recipe(id = 2L, tasteTags = setOf(TasteTag.SWEET), abv = BigDecimal("20.0"))
        val sourOnly = recipe(id = 3L, tasteTags = setOf(TasteTag.SOUR), abv = BigDecimal("20.0"))

        val result = RecipeRecommendationService.recommend(
            recipes = listOf(sweetBitter, sweetOnly, sourOnly),
            tasteTags = setOf(TastePreference.SWEET, TastePreference.BITTER),
            preferredAbv = null,
        )

        assertThat(result.map { it.id }).containsExactly(1L, 2L)
    }

    @Test
    fun `recommend - ABV 범위 매칭으로 점수 추가`() {
        val lowAbv = recipe(id = 1L, tasteTags = setOf(TasteTag.SWEET), abv = BigDecimal("10.0"))
        val highAbv = recipe(id = 2L, tasteTags = setOf(TasteTag.SWEET), abv = BigDecimal("45.0"))

        val result = RecipeRecommendationService.recommend(
            recipes = listOf(highAbv, lowAbv),
            tasteTags = setOf(TastePreference.SWEET),
            preferredAbv = PreferredAbv.LOW,
        )

        assertThat(result.map { it.id }).containsExactly(1L, 2L)
    }

    @Test
    fun `recommend - 매칭 레시피 없으면 빈 리스트`() {
        val recipe = recipe(id = 1L, tasteTags = setOf(TasteTag.STRONG))

        val result = RecipeRecommendationService.recommend(
            recipes = listOf(recipe),
            tasteTags = setOf(TastePreference.SWEET),
            preferredAbv = null,
        )

        assertThat(result).isEmpty()
    }

    @Test
    fun `recommend - limit 적용`() {
        val recipes = (1..10L).map { recipe(id = it, tasteTags = setOf(TasteTag.SWEET)) }

        val result = RecipeRecommendationService.recommend(
            recipes = recipes,
            tasteTags = setOf(TastePreference.SWEET),
            preferredAbv = null,
            limit = 3,
        )

        assertThat(result).hasSize(3)
    }

    @Test
    fun `score - taste 교집합 크기 + ABV 매칭 여부 합산`() {
        val r = recipe(tasteTags = setOf(TasteTag.SWEET, TasteTag.SOUR), abv = BigDecimal("12.0"))

        val score = RecipeRecommendationService.score(
            recipe = r,
            mappedTasteTags = setOf(TasteTag.SWEET, TasteTag.BITTER),
            preferredAbv = PreferredAbv.LOW,
        )

        assertThat(score).isEqualTo(2)
    }

    private fun recipe(
        id: Long = 1L,
        tasteTags: Set<TasteTag> = emptySet(),
        abv: BigDecimal? = null,
    ): Recipe = Recipe(
        id = id,
        name = "Recipe $id",
        category = RecipeCategory.CLASSIC,
        tasteTags = tasteTags.toMutableSet(),
        abv = abv,
    )
}
