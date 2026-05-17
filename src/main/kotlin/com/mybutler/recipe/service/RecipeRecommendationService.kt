package com.mybutler.recipe.service

import com.mybutler.recipe.entity.Recipe
import com.mybutler.recipe.entity.TasteTag
import com.mybutler.user.entity.PreferredAbv
import com.mybutler.user.entity.TastePreference
import java.math.BigDecimal

object RecipeRecommendationService {
    private val ABV_RANGES: Map<PreferredAbv, ClosedRange<BigDecimal>> = mapOf(
        PreferredAbv.LOW to BigDecimal("0.0")..BigDecimal("15.0"),
        PreferredAbv.MEDIUM to BigDecimal("15.1")..BigDecimal("30.0"),
        PreferredAbv.HIGH to BigDecimal("30.1")..BigDecimal("100.0"),
    )

    fun score(
        recipe: Recipe,
        mappedTasteTags: Set<TasteTag>,
        preferredAbv: PreferredAbv?,
    ): Int {
        val tasteScore = recipe.tasteTags.intersect(mappedTasteTags).size

        val abvScore = if (preferredAbv != null) {
            val range = ABV_RANGES[preferredAbv]
            val recipeAbv = recipe.abv
            if (range != null && recipeAbv != null && recipeAbv in range) 1 else 0
        } else {
            0
        }

        return tasteScore + abvScore
    }

    fun recommend(
        recipes: List<Recipe>,
        tasteTags: Set<TastePreference>,
        preferredAbv: PreferredAbv?,
        limit: Int = 10,
    ): List<Recipe> {
        if (tasteTags.isEmpty() && preferredAbv == null) return recipes.take(limit)
        val mappedTasteTags = tasteTags.map { it.toTasteTag() }.toSet()
        return recipes
            .map { recipe -> recipe to score(recipe, mappedTasteTags, preferredAbv) }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .take(limit)
            .map { (recipe, _) -> recipe }
    }

    private fun TastePreference.toTasteTag(): TasteTag = when (this) {
        TastePreference.SWEET -> TasteTag.SWEET
        TastePreference.SOUR -> TasteTag.SOUR
        TastePreference.BITTER -> TasteTag.BITTER
        TastePreference.STRONG -> TasteTag.STRONG
        TastePreference.LIGHT -> TasteTag.LIGHT
        TastePreference.SMOKY -> TasteTag.SMOKY
        TastePreference.SPICY -> TasteTag.SPICY
        TastePreference.FRUITY -> TasteTag.FRUITY
        TastePreference.CITRUS -> TasteTag.CITRUS
        TastePreference.DRY -> TasteTag.DRY
    }
}
