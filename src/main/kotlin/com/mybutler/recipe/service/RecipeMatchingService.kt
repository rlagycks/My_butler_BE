package com.mybutler.recipe.service

import com.mybutler.inventory.entity.InventoryItem
import com.mybutler.recipe.entity.Recipe

data class RecipeMatchResult(
    val recipe: Recipe,
    val missingCount: Int,
    val missingIngredients: List<String>,
)

object RecipeMatchingService {
    private const val NEARLY_AVAILABLE_MAX_MISSING = 2

    fun match(recipe: Recipe, inventoryItems: List<InventoryItem>): RecipeMatchResult =
        match(recipe, inventoryItems.toInventoryNameSet())

    fun match(recipe: Recipe, inventoryNames: Set<String>): RecipeMatchResult {
        val missing = recipe.ingredients.filter { ingredient ->
            val ingredientWords = ingredient.name.lowercase().toWordSet()
            inventoryNames.none { inv -> (inv.toWordSet() intersect ingredientWords).isNotEmpty() }
        }
        return RecipeMatchResult(
            recipe = recipe,
            missingCount = missing.size,
            missingIngredients = missing.map { it.name },
        )
    }

    fun partition(
        recipes: List<Recipe>,
        inventoryItems: List<InventoryItem>,
    ): Pair<List<RecipeMatchResult>, List<RecipeMatchResult>> {
        val inventoryNames = inventoryItems.toInventoryNameSet()
        val results = recipes.map { match(it, inventoryNames) }
        val available = results.filter { it.missingCount == 0 }
        val nearlyAvailable = results.filter { it.missingCount in 1..NEARLY_AVAILABLE_MAX_MISSING }
        return available to nearlyAvailable
    }

    private fun List<InventoryItem>.toInventoryNameSet(): Set<String> =
        map { it.name.lowercase() }.toSet()

    private fun String.toWordSet(): Set<String> =
        split(Regex("\\s+")).toSet()
}
