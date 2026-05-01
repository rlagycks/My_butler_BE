package com.mybutler.recipe.service

import com.mybutler.recipe.entity.Recipe
import com.mybutler.recipe.repository.RecipeRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class BaseRecipeLoader(
    private val recipeRepository: RecipeRepository,
) {
    companion object {
        private const val MAX_BASE_RECIPES = 500
    }

    @Cacheable("baseRecipes")
    fun loadAll(): List<Recipe> =
        recipeRepository.findByIsCustomFalse(PageRequest.of(0, MAX_BASE_RECIPES)).content
}
