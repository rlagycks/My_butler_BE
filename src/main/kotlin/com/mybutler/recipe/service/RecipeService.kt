package com.mybutler.recipe.service

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.common.storage.StorageService
import com.mybutler.common.util.ImageUploadValidator
import com.mybutler.common.util.deleteFromStorageQuietly
import com.mybutler.inventory.repository.InventoryItemRepository
import com.mybutler.recipe.dto.CreateRecipeRequest
import com.mybutler.recipe.dto.RecipeDetailResponse
import com.mybutler.recipe.dto.RecipeHomeResponse
import com.mybutler.recipe.dto.RecipePageResponse
import com.mybutler.recipe.dto.RecipeRecommendationResponse
import com.mybutler.recipe.dto.RecipeSummaryResponse
import com.mybutler.recipe.dto.StepRequest
import com.mybutler.recipe.dto.UpdateRecipeRequest
import com.mybutler.recipe.entity.BaseSpirit
import com.mybutler.recipe.entity.Recipe
import com.mybutler.recipe.entity.RecipeCategory
import com.mybutler.recipe.entity.RecipeIngredient
import com.mybutler.recipe.entity.RecipeStep
import com.mybutler.recipe.repository.RecipeRepository
import com.mybutler.user.repository.UserPreferenceRepository
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
@Transactional(readOnly = true)
class RecipeService(
    private val recipeRepository: RecipeRepository,
    private val inventoryItemRepository: InventoryItemRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val entityManager: EntityManager,
    private val baseRecipeLoader: BaseRecipeLoader,
    private val storageService: StorageService,
    private val imageUploadValidator: ImageUploadValidator,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getHome(userId: Long): RecipeHomeResponse {
        val inventoryItems = inventoryItemRepository.findAllByUserId(userId)
        val allBaseRecipes = baseRecipeLoader.loadAll()

        val (available, nearlyAvailable) = RecipeMatchingService.partition(allBaseRecipes, inventoryItems)

        val preference = userPreferenceRepository.findByUserId(userId).orElse(null)
        val preferenceRecs = RecipeRecommendationService.recommend(
            recipes = allBaseRecipes,
            tasteTags = preference?.tastePreferences ?: emptySet(),
            preferredAbv = preference?.preferredAbv,
            limit = 10,
        )

        return RecipeHomeResponse(
            availableRecipes = available.map { RecipeSummaryResponse.from(it.recipe) },
            nearlyAvailableRecipes = nearlyAvailable.map { RecipeSummaryResponse.from(it.recipe) },
            preferenceRecommendations = preferenceRecs.map(RecipeSummaryResponse::from),
        )
    }

    fun getBaseRecipes(
        category: RecipeCategory?,
        baseSpirit: BaseSpirit?,
        difficulty: Int?,
        keyword: String?,
        pageable: Pageable,
    ): RecipePageResponse {
        val page = recipeRepository.searchBaseRecipes(category, baseSpirit, difficulty, keyword, pageable)
        return RecipePageResponse.from(page)
    }

    fun getCustomRecipes(userId: Long, keyword: String?, pageable: Pageable): RecipePageResponse {
        val page = recipeRepository.searchCustomRecipes(userId, keyword, pageable)
        return RecipePageResponse.from(page)
    }

    fun getInventoryRecommendations(userId: Long): RecipeRecommendationResponse {
        val inventoryItems = inventoryItemRepository.findAllByUserId(userId)
        val allBaseRecipes = baseRecipeLoader.loadAll()
        val (available, nearlyAvailable) = RecipeMatchingService.partition(allBaseRecipes, inventoryItems)
        return RecipeRecommendationResponse(
            availableRecipes = available.map { RecipeSummaryResponse.from(it.recipe) },
            nearlyAvailableRecipes = nearlyAvailable.map { RecipeSummaryResponse.from(it.recipe) },
        )
    }

    fun getPreferenceRecommendations(userId: Long): List<RecipeSummaryResponse> {
        val preference = userPreferenceRepository.findByUserId(userId).orElse(null)
        val allBaseRecipes = baseRecipeLoader.loadAll()
        val recommended = RecipeRecommendationService.recommend(
            recipes = allBaseRecipes,
            tasteTags = preference?.tastePreferences ?: emptySet(),
            preferredAbv = preference?.preferredAbv,
        )
        return recommended.map(RecipeSummaryResponse::from)
    }

    fun getDetail(recipeId: Long): RecipeDetailResponse {
        val recipe = recipeRepository.findByIdOrNull(recipeId)
            ?: throw BusinessException(ErrorCode.RECIPE_NOT_FOUND)
        return RecipeDetailResponse.from(recipe)
    }

    @Transactional
    fun create(userId: Long, request: CreateRecipeRequest): RecipeDetailResponse {
        val recipe = Recipe(
            name = request.name,
            description = request.description,
            category = request.category,
            baseSpirit = request.baseSpirit,
            difficulty = request.difficulty,
            estimatedMinutes = request.estimatedMinutes,
            abv = request.abv,
            tasteTags = request.tasteTags.toMutableSet(),
            isCustom = true,
            authorId = userId,
        )

        validateStepOrders(request.steps)

        request.ingredients.forEach { req ->
            recipe.ingredients.add(
                RecipeIngredient(
                    recipe = recipe,
                    name = req.name,
                    amount = req.amount,
                    unit = req.unit,
                    displayOrder = req.displayOrder,
                ),
            )
        }

        request.steps.forEach { req ->
            recipe.steps.add(
                RecipeStep(
                    recipe = recipe,
                    stepOrder = req.stepOrder,
                    description = req.description,
                ),
            )
        }

        val saved = recipeRepository.save(recipe)
        return RecipeDetailResponse.from(saved)
    }

    @Transactional
    fun update(userId: Long, recipeId: Long, request: UpdateRecipeRequest): RecipeDetailResponse {
        val recipe = findOwnedCustom(userId, recipeId, ErrorCode.RECIPE_BASE_NOT_UPDATABLE)

        validateStepOrders(request.steps)

        recipe.name = request.name
        recipe.description = request.description
        recipe.category = request.category
        recipe.baseSpirit = request.baseSpirit
        recipe.difficulty = request.difficulty
        recipe.estimatedMinutes = request.estimatedMinutes
        recipe.abv = request.abv
        recipe.tasteTags = request.tasteTags.toMutableSet()

        recipe.ingredients.clear()
        recipe.steps.clear()
        entityManager.flush()

        request.ingredients.forEach { req ->
            recipe.ingredients.add(
                RecipeIngredient(
                    recipe = recipe,
                    name = req.name,
                    amount = req.amount,
                    unit = req.unit,
                    displayOrder = req.displayOrder,
                ),
            )
        }

        request.steps.forEach { req ->
            recipe.steps.add(
                RecipeStep(
                    recipe = recipe,
                    stepOrder = req.stepOrder,
                    description = req.description,
                ),
            )
        }

        return RecipeDetailResponse.from(recipe)
    }

    @Transactional
    fun delete(userId: Long, recipeId: Long) {
        val recipe = findOwnedCustom(userId, recipeId)
        recipeRepository.delete(recipe)
    }

    private fun validateStepOrders(steps: List<StepRequest>) {
        val orders = steps.map { it.stepOrder }
        if (orders.size != orders.toSet().size) {
            throw BusinessException(ErrorCode.RECIPE_STEP_ORDER_DUPLICATE)
        }
    }

    @Transactional
    fun uploadThumbnail(userId: Long, recipeId: Long, file: MultipartFile): RecipeDetailResponse {
        val recipe = findOwnedCustom(userId, recipeId)
        imageUploadValidator.validate(file)
        val oldThumbnailUrl = recipe.thumbnailUrl
        val newThumbnailUrl = storageService.upload(file, "recipes/thumbnails")
        recipe.thumbnailUrl = newThumbnailUrl
        oldThumbnailUrl?.takeIf { it != newThumbnailUrl }?.let {
            deleteFromStorageQuietly(storageService, it, log, "recipe thumbnail")
        }
        return RecipeDetailResponse.from(recipe)
    }

    private fun findOwnedCustom(
        userId: Long,
        recipeId: Long,
        notCustomError: ErrorCode = ErrorCode.RECIPE_BASE_NOT_DELETABLE,
    ): Recipe {
        val recipe = recipeRepository.findByIdOrNull(recipeId)
            ?: throw BusinessException(ErrorCode.RECIPE_NOT_FOUND)

        if (!recipe.isCustom) {
            throw BusinessException(notCustomError)
        }

        if (recipe.authorId != userId) {
            throw BusinessException(ErrorCode.RECIPE_ACCESS_DENIED)
        }

        return recipe
    }
}
