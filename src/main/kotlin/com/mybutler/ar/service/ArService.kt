package com.mybutler.ar.service

import com.mybutler.ar.dto.ArRecipeResponse
import com.mybutler.ar.dto.ArSessionRequest
import com.mybutler.ar.dto.ArSessionResponse
import com.mybutler.ar.entity.ArSession
import com.mybutler.ar.repository.ArSessionRepository
import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.common.storage.StorageService
import com.mybutler.common.util.ImageUploadValidator
import com.mybutler.community.entity.Post
import com.mybutler.community.entity.PostImage
import com.mybutler.community.entity.PostType
import com.mybutler.community.repository.PostRepository
import com.mybutler.recipe.entity.RecipeRating
import com.mybutler.recipe.repository.RecipeRatingRepository
import com.mybutler.recipe.repository.RecipeRepository
import org.springframework.cache.CacheManager
import org.springframework.cache.interceptor.SimpleKey
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.math.RoundingMode

private const val MAX_CAPTION_LENGTH = 500
private const val POSTS_DIRECTORY = "posts"

@Service
@Transactional(readOnly = true)
class ArService(
    private val recipeRepository: RecipeRepository,
    private val postRepository: PostRepository,
    private val recipeRatingRepository: RecipeRatingRepository,
    private val arSessionRepository: ArSessionRepository,
    private val storageService: StorageService,
    private val imageUploadValidator: ImageUploadValidator,
    private val transactionTemplate: TransactionTemplate,
    private val cacheManager: CacheManager,
) {
    fun getArRecipe(recipeId: Long): ArRecipeResponse {
        val recipe = recipeRepository.findByIdOrNull(recipeId)
            ?: throw BusinessException(ErrorCode.AR_RECIPE_NOT_FOUND)
        return ArRecipeResponse.from(recipe)
    }

    fun submitSession(userId: Long, request: ArSessionRequest, photo: MultipartFile): ArSessionResponse {
        val rating = request.rating ?: throw BusinessException(ErrorCode.AR_RATING_REQUIRED)
        if (rating < 1 || rating > 5) throw BusinessException(ErrorCode.AR_RATING_INVALID)
        if (!request.caption.isNullOrEmpty() && request.caption.length > MAX_CAPTION_LENGTH) {
            throw BusinessException(ErrorCode.AR_CAPTION_TOO_LONG)
        }

        imageUploadValidator.validate(photo)

        if (!recipeRepository.existsById(request.recipeId)) {
            throw BusinessException(ErrorCode.AR_RECIPE_NOT_FOUND)
        }

        val photoUrl = storageService.upload(photo, POSTS_DIRECTORY)

        return try {
            transactionTemplate.execute {
                val post = postRepository.save(
                    Post(
                        authorId = userId,
                        recipeId = request.recipeId,
                        type = PostType.PHOTO,
                        caption = request.caption,
                        isArGenerated = true,
                        arRating = rating.toShort(),
                    )
                )
                post.images.add(PostImage(post = post, imageUrl = photoUrl, displayOrder = 0))

                upsertRating(request.recipeId, userId, rating)

                val session = arSessionRepository.save(
                    ArSession(
                        userId = userId,
                        recipeId = request.recipeId,
                        postId = post.id,
                        rating = rating.toShort(),
                        caption = request.caption,
                        photoUrl = photoUrl,
                    )
                )

                ArSessionResponse(
                    sessionId = session.id,
                    postId = post.id,
                    recipeId = request.recipeId,
                    rating = rating,
                    caption = request.caption,
                    photoUrl = photoUrl,
                    createdAt = session.createdAt,
                )
            } ?: throw IllegalStateException("Transaction completed without creating a session")
        } catch (ex: Exception) {
            try {
                storageService.delete(photoUrl)
            } catch (deleteEx: Exception) {
                ex.addSuppressed(deleteEx)
            }
            throw ex
        }
    }

    private fun upsertRating(recipeId: Long, userId: Long, score: Int) {
        val recipe = recipeRepository.findByIdForUpdate(recipeId)
            ?: throw BusinessException(ErrorCode.AR_RECIPE_NOT_FOUND)

        val existing = recipeRatingRepository.findByRecipeIdAndUserId(recipeId, userId)
        if (existing != null) {
            existing.score = score
        } else {
            recipeRatingRepository.save(RecipeRating(recipeId = recipeId, userId = userId, score = score))
        }

        val count = recipeRatingRepository.countByRecipeId(recipeId)
        recipe.averageRating = if (count > 0) {
            BigDecimal.valueOf(recipeRatingRepository.calculateAverageScore(recipeId) ?: 0.0)
                .setScale(2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        recipe.ratingCount = count.toInt()

        if (!recipe.isCustom) {
            cacheManager.getCache("baseRecipes")?.evict(SimpleKey.EMPTY)
        }
    }
}
