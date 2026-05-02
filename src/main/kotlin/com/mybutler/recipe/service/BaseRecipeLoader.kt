package com.mybutler.recipe.service

import com.mybutler.recipe.entity.Recipe
import com.mybutler.recipe.repository.RecipeRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BaseRecipeLoader(
    private val recipeRepository: RecipeRepository,
) {
    companion object {
        private const val MAX_BASE_RECIPES = 500
    }

    @Cacheable("baseRecipes", sync = true)
    @Transactional(readOnly = true)
    fun loadAll(): List<Recipe> {
        val recipes = recipeRepository.findByIsCustomFalse(PageRequest.of(0, MAX_BASE_RECIPES)).content
        // Force-initialize lazy collections while the session is still open so cached detached entities remain usable
        recipes.forEach { recipe ->
            recipe.ingredients.size
            recipe.tasteTags.size
            recipe.steps.size
        }
        return recipes
    }
}
