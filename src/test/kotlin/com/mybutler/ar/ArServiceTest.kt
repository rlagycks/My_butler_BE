package com.mybutler.ar

import com.mybutler.ar.dto.ArSessionRequest
import com.mybutler.ar.entity.ArSession
import com.mybutler.ar.repository.ArSessionRepository
import com.mybutler.ar.service.ArService
import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.common.storage.StorageService
import com.mybutler.common.util.ImageUploadValidator
import com.mybutler.community.entity.Post
import com.mybutler.community.entity.PostType
import com.mybutler.community.repository.PostRepository
import com.mybutler.recipe.entity.Recipe
import com.mybutler.recipe.entity.RecipeCategory
import com.mybutler.recipe.entity.RecipeIngredient
import com.mybutler.recipe.entity.RecipeRating
import com.mybutler.recipe.entity.RecipeStep
import com.mybutler.recipe.repository.RecipeRatingRepository
import com.mybutler.recipe.repository.RecipeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.cache.CacheManager
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.cache.interceptor.SimpleKey
import org.springframework.mock.web.MockMultipartFile
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ArServiceTest {

    @Mock lateinit var recipeRepository: RecipeRepository
    @Mock lateinit var postRepository: PostRepository
    @Mock lateinit var recipeRatingRepository: RecipeRatingRepository
    @Mock lateinit var arSessionRepository: ArSessionRepository
    @Mock lateinit var storageService: StorageService
    @Mock lateinit var transactionTemplate: TransactionTemplate

    private lateinit var arService: ArService
    private val imageUploadValidator = ImageUploadValidator()
    private val cacheManager: CacheManager = ConcurrentMapCacheManager("baseRecipes")

    private val jpegPhoto = MockMultipartFile("photo", "photo.jpg", "image/jpeg", "data".toByteArray())

    @BeforeEach
    fun setUp() {
        arService = ArService(
            recipeRepository, postRepository, recipeRatingRepository,
            arSessionRepository, storageService, imageUploadValidator, transactionTemplate, cacheManager,
        )
    }

    @Test
    fun `getArRecipe - 레시피 반환`() {
        val recipe = recipe(id = 1L)
        given(recipeRepository.findById(1L)).willReturn(Optional.of(recipe))

        val result = arService.getArRecipe(1L)

        assertThat(result.recipeId).isEqualTo(1L)
        assertThat(result.name).isEqualTo("Mojito")
    }

    @Test
    fun `getArRecipe - 없는 레시피 AR_RECIPE_NOT_FOUND 예외`() {
        given(recipeRepository.findById(999L)).willReturn(Optional.empty())

        val ex = assertThrows<BusinessException> { arService.getArRecipe(999L) }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.AR_RECIPE_NOT_FOUND)
    }

    @Test
    fun `submitSession - 별점 없으면 AR_RATING_REQUIRED 예외`() {
        val request = ArSessionRequest(recipeId = 1L, rating = null, caption = null)

        val ex = assertThrows<BusinessException> { arService.submitSession(1L, request, jpegPhoto) }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.AR_RATING_REQUIRED)
        verify(storageService, never()).upload(any(), any())
    }

    @Test
    fun `submitSession - 범위 밖 별점 AR_RATING_INVALID 예외`() {
        val request = ArSessionRequest(recipeId = 1L, rating = 6, caption = null)

        val ex = assertThrows<BusinessException> { arService.submitSession(1L, request, jpegPhoto) }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.AR_RATING_INVALID)
    }

    @Test
    fun `submitSession - 500자 초과 캡션 AR_CAPTION_TOO_LONG 예외`() {
        val request = ArSessionRequest(recipeId = 1L, rating = 4, caption = "a".repeat(501))

        val ex = assertThrows<BusinessException> { arService.submitSession(1L, request, jpegPhoto) }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.AR_CAPTION_TOO_LONG)
    }

    @Test
    fun `submitSession - 없는 레시피 AR_RECIPE_NOT_FOUND 예외`() {
        val request = ArSessionRequest(recipeId = 999L, rating = 4, caption = null)
        given(recipeRepository.existsById(999L)).willReturn(false)

        val ex = assertThrows<BusinessException> { arService.submitSession(1L, request, jpegPhoto) }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.AR_RECIPE_NOT_FOUND)
        verify(storageService, never()).upload(any(), any())
    }

    @Test
    fun `submitSession - 이미지 아니면 INVALID_FILE_TYPE 예외`() {
        val txtFile = MockMultipartFile("photo", "note.txt", "text/plain", "data".toByteArray())
        val request = ArSessionRequest(recipeId = 1L, rating = 4, caption = null)

        val ex = assertThrows<BusinessException> { arService.submitSession(1L, request, txtFile) }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.INVALID_FILE_TYPE)
        verify(storageService, never()).upload(any(), any())
    }

    @Test
    fun `submitSession - 트랜잭션 실패 시 업로드된 사진 삭제`() {
        val request = ArSessionRequest(recipeId = 1L, rating = 4, caption = null)
        given(recipeRepository.existsById(1L)).willReturn(true)
        given(storageService.upload(any(), any())).willReturn("posts/photo.jpg")
        given(transactionTemplate.execute(any<TransactionCallback<*>>())).willThrow(RuntimeException("DB error"))

        assertThrows<RuntimeException> { arService.submitSession(1L, request, jpegPhoto) }

        verify(storageService).delete("posts/photo.jpg")
    }

    @Test
    fun `submitSession - 성공 시 ArSessionResponse 반환`() {
        val request = ArSessionRequest(recipeId = 1L, rating = 4, caption = "Good!")
        val recipe = recipe(id = 1L)
        val createdAt = LocalDateTime.now()
        cacheManager.getCache("baseRecipes")!!.put(SimpleKey.EMPTY, listOf(recipe))

        given(recipeRepository.existsById(1L)).willReturn(true)
        given(recipeRepository.findByIdForUpdate(1L)).willReturn(recipe)
        given(storageService.upload(any(), any())).willReturn("posts/photo.jpg")
        given(postRepository.save(any<Post>())).willReturn(
            Post(
                id = 7L,
                authorId = 1L,
                recipeId = 1L,
                type = PostType.PHOTO,
                caption = "Good!",
                isArGenerated = true,
                arRating = 4,
            ),
        )
        given(recipeRatingRepository.findByRecipeIdAndUserId(1L, 1L)).willReturn(null)
        given(recipeRatingRepository.save(any<RecipeRating>())).willReturn(RecipeRating(recipeId = 1L, userId = 1L, score = 4))
        given(recipeRatingRepository.countByRecipeId(1L)).willReturn(1L)
        given(recipeRatingRepository.calculateAverageScore(1L)).willReturn(4.0)
        given(arSessionRepository.save(any<ArSession>())).willAnswer {
            val session = it.getArgument<ArSession>(0)
            ArSession(
                id = 3L,
                userId = session.userId,
                recipeId = session.recipeId,
                postId = session.postId,
                rating = session.rating,
                caption = session.caption,
                photoUrl = session.photoUrl,
                createdAt = createdAt,
            )
        }
        given(transactionTemplate.execute(any<TransactionCallback<*>>())).willAnswer {
            val callback = it.getArgument<TransactionCallback<Any?>>(0)
            callback.doInTransaction(mock<TransactionStatus>())
        }

        val result = arService.submitSession(1L, request, jpegPhoto)

        assertThat(result.sessionId).isEqualTo(3L)
        assertThat(result.postId).isEqualTo(7L)
        assertThat(result.rating).isEqualTo(4)
        assertThat(recipe.averageRating).isEqualByComparingTo(BigDecimal("4.00"))
        assertThat(recipe.ratingCount).isEqualTo(1)
        assertThat(cacheManager.getCache("baseRecipes")!!.get(SimpleKey.EMPTY)).isNull()
        verify(storageService, never()).delete(any())
    }

    private fun recipe(id: Long = 1L) = Recipe(
        id = id,
        name = "Mojito",
        category = RecipeCategory.CLASSIC,
        difficulty = 1,
        isCustom = false,
    )
}
