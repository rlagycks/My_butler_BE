package com.mybutler.recipe

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.inventory.entity.Category
import com.mybutler.inventory.entity.InventoryItem
import com.mybutler.inventory.entity.LevelStatus
import com.mybutler.inventory.repository.InventoryItemRepository
import com.mybutler.recipe.dto.CreateRecipeRequest
import com.mybutler.recipe.dto.IngredientRequest
import com.mybutler.recipe.dto.StepRequest
import com.mybutler.recipe.dto.UpdateRecipeRequest
import com.mybutler.recipe.entity.Recipe
import com.mybutler.recipe.entity.RecipeCategory
import com.mybutler.recipe.entity.RecipeIngredient
import com.mybutler.recipe.entity.TasteTag
import com.mybutler.recipe.repository.RecipeRepository
import com.mybutler.common.storage.StorageService
import com.mybutler.common.util.ImageUploadValidator
import com.mybutler.recipe.service.BaseRecipeLoader
import com.mybutler.recipe.service.RecipeService
import com.mybutler.user.repository.UserPreferenceRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.willThrow
import org.springframework.mock.web.MockMultipartFile
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class RecipeServiceTest {

    @Mock lateinit var recipeRepository: RecipeRepository
    @Mock lateinit var inventoryItemRepository: InventoryItemRepository
    @Mock lateinit var userPreferenceRepository: UserPreferenceRepository
    @Mock lateinit var entityManager: EntityManager
    @Mock lateinit var baseRecipeLoader: BaseRecipeLoader
    @Mock lateinit var storageService: StorageService

    private lateinit var recipeService: RecipeService
    private val imageUploadValidator = ImageUploadValidator()

    @BeforeEach
    fun setUp() {
        recipeService = RecipeService(
            recipeRepository,
            inventoryItemRepository,
            userPreferenceRepository,
            entityManager,
            baseRecipeLoader,
            storageService,
            imageUploadValidator,
        )
    }

    @Test
    fun `getDetail - 레시피 상세 반환`() {
        val recipeId = 1L
        val recipe = recipe(id = recipeId, name = "Whiskey Sour")

        given(recipeRepository.findById(recipeId)).willReturn(Optional.of(recipe))

        val result = recipeService.getDetail(recipeId)

        assertThat(result.id).isEqualTo(recipeId)
        assertThat(result.name).isEqualTo("Whiskey Sour")
    }

    @Test
    fun `getDetail - 존재하지 않는 레시피 RECIPE_NOT_FOUND 예외`() {
        given(recipeRepository.findById(999L)).willReturn(Optional.empty())

        val ex = assertThrows<BusinessException> {
            recipeService.getDetail(999L)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.RECIPE_NOT_FOUND)
    }

    @Test
    fun `create - 나만의 레시피 생성 후 isCustom=true, authorId 설정`() {
        val userId = 1L
        val request = CreateRecipeRequest(
            name = "My Cocktail",
            category = RecipeCategory.CLASSIC,
            tasteTags = setOf(TasteTag.SWEET),
            ingredients = listOf(IngredientRequest(name = "Vodka", amount = "50", unit = "ml")),
            steps = listOf(StepRequest(stepOrder = 1, description = "Mix everything")),
        )
        val captor = argumentCaptor<Recipe>()
        val saved = recipe(id = 100L, name = request.name, isCustom = true, authorId = userId)

        given(recipeRepository.save(any<Recipe>())).willReturn(saved)

        val result = recipeService.create(userId, request)

        verify(recipeRepository).save(captor.capture())
        assertThat(captor.firstValue.isCustom).isTrue()
        assertThat(captor.firstValue.authorId).isEqualTo(userId)
        assertThat(captor.firstValue.tasteTags).containsExactlyInAnyOrder(TasteTag.SWEET)
        assertThat(result.id).isEqualTo(100L)
        assertThat(result.isCustom).isTrue()
    }

    @Test
    fun `update - 존재하지 않는 레시피 RECIPE_NOT_FOUND 예외`() {
        given(recipeRepository.findById(999L)).willReturn(Optional.empty())

        val ex = assertThrows<BusinessException> {
            recipeService.update(1L, 999L, updateRequest())
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.RECIPE_NOT_FOUND)
    }

    @Test
    fun `update - 기본 레시피 수정 시 RECIPE_BASE_NOT_UPDATABLE 예외`() {
        val recipeId = 1L
        val baseRecipe = recipe(id = recipeId, isCustom = false)

        given(recipeRepository.findById(recipeId)).willReturn(Optional.of(baseRecipe))

        val ex = assertThrows<BusinessException> {
            recipeService.update(1L, recipeId, updateRequest())
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.RECIPE_BASE_NOT_UPDATABLE)
    }

    @Test
    fun `update - 다른 사용자의 커스텀 레시피 RECIPE_ACCESS_DENIED 예외`() {
        val recipeId = 1L
        val customRecipe = recipe(id = recipeId, isCustom = true, authorId = 2L)

        given(recipeRepository.findById(recipeId)).willReturn(Optional.of(customRecipe))

        val ex = assertThrows<BusinessException> {
            recipeService.update(userId = 1L, recipeId = recipeId, request = updateRequest())
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.RECIPE_ACCESS_DENIED)
    }

    @Test
    fun `update - 본인 커스텀 레시피 정상 수정`() {
        val userId = 1L
        val recipeId = 1L
        val existingRecipe = recipe(id = recipeId, name = "Old Name", isCustom = true, authorId = userId)

        given(recipeRepository.findById(recipeId)).willReturn(Optional.of(existingRecipe))

        val request = updateRequest(name = "New Name")
        val result = recipeService.update(userId, recipeId, request)

        assertThat(existingRecipe.name).isEqualTo("New Name")
        assertThat(result.name).isEqualTo("New Name")
    }

    @Test
    fun `delete - 기본 레시피 삭제 시 RECIPE_BASE_NOT_DELETABLE 예외`() {
        val recipeId = 1L
        given(recipeRepository.findById(recipeId)).willReturn(Optional.of(recipe(id = recipeId, isCustom = false)))

        val ex = assertThrows<BusinessException> {
            recipeService.delete(userId = 1L, recipeId = recipeId)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.RECIPE_BASE_NOT_DELETABLE)
    }

    @Test
    fun `delete - 본인 커스텀 레시피 삭제 성공`() {
        val userId = 1L
        val recipeId = 1L
        val customRecipe = recipe(id = recipeId, isCustom = true, authorId = userId)

        given(recipeRepository.findById(recipeId)).willReturn(Optional.of(customRecipe))

        recipeService.delete(userId, recipeId)

        verify(recipeRepository).delete(customRecipe)
    }

    @Test
    fun `getHome - 재고 없으면 available 비어 있고 preference 추천 반환`() {
        val userId = 1L

        given(inventoryItemRepository.findAllByUserId(userId)).willReturn(emptyList())
        given(baseRecipeLoader.loadAll()).willReturn(emptyList())
        given(userPreferenceRepository.findByUserId(userId)).willReturn(Optional.empty())

        val result = recipeService.getHome(userId)

        assertThat(result.availableRecipes).isEmpty()
        assertThat(result.nearlyAvailableRecipes).isEmpty()
        assertThat(result.preferenceRecommendations).isEmpty()
    }

    @Test
    fun `getInventoryRecommendations - 재고 기반 분류 반환`() {
        val userId = 1L
        val inventoryItems = listOf(inventoryItem("Whiskey"), inventoryItem("Lemon Juice"))
        val whiskeyRecipe = recipe(id = 1L, ingredients = listOf("Whiskey", "Lemon Juice"))
        val ginRecipe = recipe(id = 2L, ingredients = listOf("Gin", "Tonic"))

        given(inventoryItemRepository.findAllByUserId(userId)).willReturn(inventoryItems)
        given(baseRecipeLoader.loadAll()).willReturn(listOf(whiskeyRecipe, ginRecipe))

        val result = recipeService.getInventoryRecommendations(userId)

        assertThat(result.availableRecipes.map { it.id }).containsExactly(1L)
        assertThat(result.nearlyAvailableRecipes.map { it.id }).containsExactly(2L)
    }

    @Test
    fun `uploadThumbnail - 썸네일 업로드 후 URL 반환`() {
        val userId = 1L
        val recipeId = 1L
        val customRecipe = recipe(id = recipeId, isCustom = true, authorId = userId)
        val file = MockMultipartFile("file", "thumb.jpg", "image/jpeg", "data".toByteArray())

        given(recipeRepository.findById(recipeId)).willReturn(Optional.of(customRecipe))
        given(storageService.upload(any(), any())).willReturn("recipes/thumbnails/uuid.jpg")

        val result = recipeService.uploadThumbnail(userId, recipeId, file)

        assertThat(result.thumbnailUrl).isEqualTo("recipes/thumbnails/uuid.jpg")
        verify(storageService).upload(file, "recipes/thumbnails")
    }

    @Test
    fun `uploadThumbnail - 기존 썸네일 있으면 새 업로드 후 이전 파일 삭제`() {
        val userId = 1L
        val recipeId = 1L
        val customRecipe = recipe(id = recipeId, isCustom = true, authorId = userId, thumbnailUrl = "recipes/thumbnails/old.jpg")
        val file = MockMultipartFile("file", "new.jpg", "image/jpeg", "data".toByteArray())

        given(recipeRepository.findById(recipeId)).willReturn(Optional.of(customRecipe))
        given(storageService.upload(any(), any())).willReturn("recipes/thumbnails/new-uuid.jpg")

        recipeService.uploadThumbnail(userId, recipeId, file)

        inOrder(storageService) {
            verify(storageService).upload(file, "recipes/thumbnails")
            verify(storageService).delete("recipes/thumbnails/old.jpg")
        }
    }

    @Test
    fun `uploadThumbnail - 기존 썸네일 삭제 실패여도 새 URL로 응답한다`() {
        val userId = 1L
        val recipeId = 1L
        val customRecipe = recipe(id = recipeId, isCustom = true, authorId = userId, thumbnailUrl = "recipes/thumbnails/old.jpg")
        val file = MockMultipartFile("file", "new.jpg", "image/jpeg", "data".toByteArray())

        given(recipeRepository.findById(recipeId)).willReturn(Optional.of(customRecipe))
        given(storageService.upload(any(), any())).willReturn("recipes/thumbnails/new-uuid.jpg")
        given(storageService.delete("recipes/thumbnails/old.jpg")).willThrow(RuntimeException("delete failed"))

        val result = recipeService.uploadThumbnail(userId, recipeId, file)

        assertThat(result.thumbnailUrl).isEqualTo("recipes/thumbnails/new-uuid.jpg")
        assertThat(customRecipe.thumbnailUrl).isEqualTo("recipes/thumbnails/new-uuid.jpg")
    }

    @Test
    fun `uploadThumbnail - 존재하지 않는 레시피 RECIPE_NOT_FOUND 예외`() {
        given(recipeRepository.findById(any())).willReturn(Optional.empty())
        val file = MockMultipartFile("file", "thumb.jpg", "image/jpeg", "data".toByteArray())

        val ex = assertThrows<BusinessException> {
            recipeService.uploadThumbnail(1L, 999L, file)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.RECIPE_NOT_FOUND)
    }

    @Test
    fun `uploadThumbnail - 다른 사용자의 레시피 RECIPE_ACCESS_DENIED 예외`() {
        val recipeId = 1L
        val customRecipe = recipe(id = recipeId, isCustom = true, authorId = 2L)
        val file = MockMultipartFile("file", "thumb.jpg", "image/jpeg", "data".toByteArray())

        given(recipeRepository.findById(recipeId)).willReturn(Optional.of(customRecipe))

        val ex = assertThrows<BusinessException> {
            recipeService.uploadThumbnail(userId = 1L, recipeId = recipeId, file = file)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.RECIPE_ACCESS_DENIED)
    }

    @Test
    fun `uploadThumbnail - 이미지가 아니면 INVALID_FILE_TYPE 예외`() {
        val userId = 1L
        val recipeId = 1L
        val customRecipe = recipe(id = recipeId, isCustom = true, authorId = userId)
        val file = MockMultipartFile("file", "thumb.txt", "text/plain", "data".toByteArray())

        given(recipeRepository.findById(recipeId)).willReturn(Optional.of(customRecipe))

        val ex = assertThrows<BusinessException> {
            recipeService.uploadThumbnail(userId, recipeId, file)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.INVALID_FILE_TYPE)
        verify(storageService, never()).upload(any(), any())
    }

    private fun recipe(
        id: Long = 1L,
        name: String = "Test Recipe",
        isCustom: Boolean = false,
        authorId: Long? = null,
        ingredients: List<String> = emptyList(),
        thumbnailUrl: String? = null,
    ): Recipe {
        val r = Recipe(
            id = id,
            name = name,
            category = RecipeCategory.CLASSIC,
            isCustom = isCustom,
            authorId = authorId,
            thumbnailUrl = thumbnailUrl,
        )
        ingredients.forEachIndexed { i, ingredientName ->
            r.ingredients.add(RecipeIngredient(recipe = r, name = ingredientName, displayOrder = i))
        }
        return r
    }

    private fun inventoryItem(name: String): InventoryItem =
        InventoryItem(userId = 1L, name = name, category = Category.WHISKEY, levelStatus = LevelStatus.FULL)

    private fun updateRequest(name: String = "Updated"): UpdateRecipeRequest =
        UpdateRecipeRequest(
            name = name,
            category = RecipeCategory.CLASSIC,
            ingredients = listOf(IngredientRequest(name = "Vodka")),
            steps = listOf(StepRequest(stepOrder = 1, description = "Mix")),
        )
}
