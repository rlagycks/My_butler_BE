package com.mybutler.recipe.dto

import com.mybutler.recipe.entity.BaseSpirit
import com.mybutler.recipe.entity.RecipeCategory
import com.mybutler.recipe.entity.TasteTag
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateRecipeRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val name: String,

    val description: String? = null,

    val category: RecipeCategory,

    val baseSpirit: BaseSpirit? = null,

    @field:Min(1)
    @field:Max(3)
    val difficulty: Int = 1,

    val estimatedMinutes: Int? = null,

    @field:DecimalMin("0.0")
    @field:DecimalMax("100.0")
    val abv: BigDecimal? = null,

    val tasteTags: Set<TasteTag> = emptySet(),

    @field:NotEmpty
    @field:Valid
    val ingredients: List<IngredientRequest>,

    @field:NotEmpty
    @field:Valid
    val steps: List<StepRequest>,
)

data class UpdateRecipeRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val name: String,

    val description: String? = null,

    val category: RecipeCategory,

    val baseSpirit: BaseSpirit? = null,

    @field:Min(1)
    @field:Max(3)
    val difficulty: Int = 1,

    val estimatedMinutes: Int? = null,

    @field:DecimalMin("0.0")
    @field:DecimalMax("100.0")
    val abv: BigDecimal? = null,

    val tasteTags: Set<TasteTag> = emptySet(),

    @field:NotEmpty
    @field:Valid
    val ingredients: List<IngredientRequest>,

    @field:NotEmpty
    @field:Valid
    val steps: List<StepRequest>,
)

data class IngredientRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val name: String,

    @field:Size(max = 32)
    val amount: String? = null,

    @field:Size(max = 32)
    val unit: String? = null,

    val displayOrder: Int = 0,
)

data class StepRequest(
    @field:Min(1)
    val stepOrder: Int,

    @field:NotBlank
    val description: String,
)
