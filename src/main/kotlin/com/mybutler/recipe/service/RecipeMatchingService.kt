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

    fun match(recipe: Recipe, inventoryItems: List<InventoryItem>): RecipeMatchResult {
        val inventoryNames = inventoryItems.map { it.name.lowercase() }.toSet()
        val missing = recipe.ingredients.filter { ingredient ->
            val ingredientLower = ingredient.name.lowercase()
            inventoryNames.none { inv -> inv.contains(ingredientLower) || ingredientLower.contains(inv) }
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
        val results = recipes.map { match(it, inventoryItems) }
        val available = results.filter { it.missingCount == 0 }
        val nearlyAvailable = results.filter { it.missingCount in 1..NEARLY_AVAILABLE_MAX_MISSING }
        return available to nearlyAvailable
    }
}
