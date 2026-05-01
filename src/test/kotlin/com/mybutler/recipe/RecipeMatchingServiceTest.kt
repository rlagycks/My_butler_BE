package com.mybutler.recipe

import com.mybutler.inventory.entity.Category
import com.mybutler.inventory.entity.InventoryItem
import com.mybutler.inventory.entity.LevelStatus
import com.mybutler.recipe.entity.Recipe
import com.mybutler.recipe.entity.RecipeCategory
import com.mybutler.recipe.entity.RecipeIngredient
import com.mybutler.recipe.service.RecipeMatchingService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class RecipeMatchingServiceTest {

    @Test
    fun `match - 모든 재료를 보유한 경우 missingCount=0`() {
        val recipe = recipe(ingredients = listOf("Whiskey", "Lemon Juice", "Simple Syrup"))
        val inventory = listOf(
            inventoryItem("Jameson Whiskey"),
            inventoryItem("Lemon Juice"),
            inventoryItem("Simple Syrup"),
        )

        val result = RecipeMatchingService.match(recipe, inventory)

        assertThat(result.missingCount).isEqualTo(0)
        assertThat(result.missingIngredients).isEmpty()
    }

    @Test
    fun `match - 일부 재료 부족 시 missingCount 반환`() {
        val recipe = recipe(ingredients = listOf("Rum", "Lime Juice", "Mint", "Soda Water"))
        val inventory = listOf(
            inventoryItem("White Rum"),
            inventoryItem("Lime Juice"),
        )

        val result = RecipeMatchingService.match(recipe, inventory)

        assertThat(result.missingCount).isEqualTo(2)
        assertThat(result.missingIngredients).containsExactlyInAnyOrder("Mint", "Soda Water")
    }

    @Test
    fun `match - 재고가 없는 경우 모든 재료 missing`() {
        val recipe = recipe(ingredients = listOf("Gin", "Tonic Water"))
        val inventory = emptyList<InventoryItem>()

        val result = RecipeMatchingService.match(recipe, inventory)

        assertThat(result.missingCount).isEqualTo(2)
        assertThat(result.missingIngredients).containsExactlyInAnyOrder("Gin", "Tonic Water")
    }

    @Test
    fun `match - 대소문자 무시하고 이름 부분 매칭`() {
        val recipe = recipe(ingredients = listOf("whiskey"))
        val inventory = listOf(inventoryItem("Jameson WHISKEY"))

        val result = RecipeMatchingService.match(recipe, inventory)

        assertThat(result.missingCount).isEqualTo(0)
    }

    @Test
    fun `match - 재고명이 재료명보다 짧아도 포함 시 매칭`() {
        val recipe = recipe(ingredients = listOf("Jameson Whiskey"))
        val inventory = listOf(inventoryItem("Whiskey"))

        val result = RecipeMatchingService.match(recipe, inventory)

        assertThat(result.missingCount).isEqualTo(0)
    }

    @Test
    fun `partition - available과 nearlyAvailable 올바르게 분류`() {
        val r0 = recipe(id = 1L, ingredients = listOf("Vodka"))
        val r1 = recipe(id = 2L, ingredients = listOf("Vodka", "Lime Juice"))
        val r2 = recipe(id = 3L, ingredients = listOf("Vodka", "Tonic", "Lime"))
        val r3 = recipe(id = 4L, ingredients = listOf("Rum", "Mint", "Lime Juice", "Sugar", "Soda"))

        val inventory = listOf(inventoryItem("Vodka"))

        val (available, nearlyAvailable) = RecipeMatchingService.partition(listOf(r0, r1, r2, r3), inventory)

        assertThat(available.map { it.recipe.id }).containsExactly(1L)
        assertThat(nearlyAvailable.map { it.recipe.id }).containsExactlyInAnyOrder(2L, 3L)
    }

    private fun recipe(
        id: Long = 1L,
        ingredients: List<String> = emptyList(),
    ): Recipe {
        val r = Recipe(
            id = id,
            name = "Test Recipe",
            category = RecipeCategory.CLASSIC,
        )
        ingredients.forEachIndexed { i, name ->
            r.ingredients.add(
                RecipeIngredient(recipe = r, name = name, displayOrder = i),
            )
        }
        return r
    }

    private fun inventoryItem(name: String): InventoryItem =
        InventoryItem(
            userId = 1L,
            name = name,
            category = Category.WHISKEY,
            levelStatus = LevelStatus.FULL,
        )
}
