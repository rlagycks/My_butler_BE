package com.mybutler.recipe.repository

import com.mybutler.recipe.entity.RecipeRating
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface RecipeRatingRepository : JpaRepository<RecipeRating, Long> {

    fun findByRecipeIdAndUserId(recipeId: Long, userId: Long): RecipeRating?

    fun findByRecipeId(recipeId: Long, pageable: Pageable): Page<RecipeRating>

    fun countByRecipeId(recipeId: Long): Long

    @Query("SELECT AVG(r.score) FROM RecipeRating r WHERE r.recipeId = :recipeId")
    fun calculateAverageScore(recipeId: Long): Double?

    fun deleteByRecipeIdAndUserId(recipeId: Long, userId: Long): Long
}
