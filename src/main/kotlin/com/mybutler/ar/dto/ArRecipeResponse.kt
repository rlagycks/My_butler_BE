package com.mybutler.ar.dto

import com.mybutler.recipe.entity.Recipe

data class ArRecipeResponse(
    val recipeId: Long,
    val name: String,
    val totalVolumeMl: Double,
    val ingredients: List<ArIngredientDto>,
    val steps: List<ArStepDto>,
) {
    companion object {
        fun from(recipe: Recipe): ArRecipeResponse {
            val ingredients = recipe.ingredients.map { ArIngredientDto.from(it) }
            val totalVolumeMl = ingredients.sumOf { it.amountMl }
            return ArRecipeResponse(
                recipeId = recipe.id,
                name = recipe.name,
                totalVolumeMl = totalVolumeMl,
                ingredients = ingredients,
                steps = recipe.steps.map { ArStepDto(order = it.stepOrder, description = it.description) },
            )
        }
    }
}

data class ArIngredientDto(
    val order: Int,
    val name: String,
    val amountMl: Double,
    val unit: String,
) {
    companion object {
        fun from(ingredient: com.mybutler.recipe.entity.RecipeIngredient): ArIngredientDto {
            val unit = ingredient.unit?.lowercase() ?: "ml"
            val rawAmount = ingredient.amount?.toDoubleOrNull() ?: 0.0
            val amountMl = when (unit) {
                "oz" -> rawAmount * 29.5735
                "cl" -> rawAmount * 10.0
                "tsp" -> rawAmount * 5.0
                "tbsp" -> rawAmount * 15.0
                else -> rawAmount
            }
            return ArIngredientDto(
                order = ingredient.displayOrder,
                name = ingredient.name,
                amountMl = amountMl,
                unit = unit,
            )
        }
    }
}

data class ArStepDto(
    val order: Int,
    val description: String,
)
