package com.mybutler.recipe.repository

import com.mybutler.recipe.entity.BaseSpirit
import com.mybutler.recipe.entity.Recipe
import com.mybutler.recipe.entity.RecipeCategory
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface RecipeRepository : JpaRepository<Recipe, Long> {
    fun findByIsCustomFalse(pageable: Pageable): Page<Recipe>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Recipe r WHERE r.id = :id")
    fun findByIdForUpdate(id: Long): Recipe?

    fun findByIsCustomFalseAndCategory(category: RecipeCategory, pageable: Pageable): Page<Recipe>

    @Query(
        """
        SELECT r FROM Recipe r
        WHERE r.isCustom = false
          AND (:category IS NULL OR r.category = :category)
          AND (:baseSpirit IS NULL OR r.baseSpirit = :baseSpirit)
          AND (:difficulty IS NULL OR r.difficulty = :difficulty)
          AND (:keyword IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """,
    )
    fun searchBaseRecipes(
        category: RecipeCategory?,
        baseSpirit: BaseSpirit?,
        difficulty: Int?,
        keyword: String?,
        pageable: Pageable,
    ): Page<Recipe>

    @Query(
        """
        SELECT r FROM Recipe r
        WHERE r.isCustom = true AND r.authorId = :authorId
          AND (:keyword IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """,
    )
    fun searchCustomRecipes(
        authorId: Long,
        keyword: String?,
        pageable: Pageable,
    ): Page<Recipe>

    fun findByIsCustomTrueAndAuthorId(authorId: Long, pageable: Pageable): Page<Recipe>

    fun countByIsCustomTrueAndAuthorId(authorId: Long): Long
}
