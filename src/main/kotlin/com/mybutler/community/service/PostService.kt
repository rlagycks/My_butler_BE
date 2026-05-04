package com.mybutler.community.service

import com.mybutler.auth.repository.UserRepository
import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.common.storage.StorageService
import com.mybutler.common.util.ImageUploadValidator
import com.mybutler.community.dto.AuthorDto
import com.mybutler.community.dto.CommentPageResponse
import com.mybutler.community.dto.CreatePostRequest
import com.mybutler.community.dto.FeedPageResponse
import com.mybutler.community.dto.MyPostPageResponse
import com.mybutler.community.dto.MyPostSummaryResponse
import com.mybutler.community.dto.MyProfileResponse
import com.mybutler.community.dto.MyProfileStatsResponse
import com.mybutler.community.dto.MyProfileUserResponse
import com.mybutler.community.dto.PostCreateResponse
import com.mybutler.community.dto.PostDetailPostResponse
import com.mybutler.community.dto.PostDetailResponse
import com.mybutler.community.dto.PostSummaryResponse
import com.mybutler.community.dto.RecipeTagDto
import com.mybutler.community.entity.Post
import com.mybutler.community.entity.PostImage
import com.mybutler.community.entity.PostType
import com.mybutler.community.repository.PostCommentRepository
import com.mybutler.community.repository.PostLikeRepository
import com.mybutler.community.repository.PostRepository
import com.mybutler.recipe.repository.RecipeRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.multipart.MultipartFile
import org.slf4j.LoggerFactory

private const val MAX_CAPTION_LENGTH = 2000
private const val MAX_IMAGE_COUNT = 10
private const val POSTS_DIRECTORY = "posts"

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val postLikeRepository: PostLikeRepository,
    private val postCommentRepository: PostCommentRepository,
    private val userRepository: UserRepository,
    private val recipeRepository: RecipeRepository,
    private val storageService: StorageService,
    private val imageUploadValidator: ImageUploadValidator,
    private val postCommentService: PostCommentService,
    private val transactionTemplate: TransactionTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getPostFeed(currentUserId: Long, sort: String, pageable: Pageable): FeedPageResponse {
        val page = when (sort.uppercase()) {
            "POPULAR" -> postRepository.findAllForFeedPopular(pageable)
            else -> postRepository.findAllForFeedLatest(pageable)
        }
        return buildFeedPageResponse(page.content, currentUserId, page.number, page.size, page.totalElements, page.totalPages, page.isLast)
    }

    fun getMyPosts(currentUserId: Long, pageable: Pageable): FeedPageResponse {
        val page = postRepository.findAllByAuthorId(currentUserId, pageable)
        return buildFeedPageResponse(page.content, currentUserId, page.number, page.size, page.totalElements, page.totalPages, page.isLast)
    }

    fun getMyProfile(currentUserId: Long): MyProfileResponse {
        val user = userRepository.findById(currentUserId).orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val postCount = postRepository.countByAuthorId(currentUserId)
        val receivedLikeCount = postRepository.sumLikeCountByAuthorId(currentUserId)
        val myRecipeCount = recipeRepository.countByIsCustomTrueAndAuthorId(currentUserId)

        val initialPage = postRepository.findAllByAuthorId(currentUserId, PageRequest.of(0, 9))
        val posts = MyPostPageResponse(
            content = initialPage.content.map { post ->
                MyPostSummaryResponse(
                    id = post.id,
                    type = post.type,
                    imageUrls = post.images.map { it.imageUrl },
                    likeCount = post.likeCount,
                    commentCount = post.commentCount,
                    createdAt = post.createdAt,
                )
            },
            page = initialPage.number,
            size = initialPage.size,
            totalElements = initialPage.totalElements,
            totalPages = initialPage.totalPages,
            last = initialPage.isLast,
        )

        return MyProfileResponse(
            profile = MyProfileUserResponse(id = user.id, username = user.username, email = user.email),
            stats = MyProfileStatsResponse(
                postCount = postCount,
                receivedLikeCount = receivedLikeCount,
                myRecipeCount = myRecipeCount,
            ),
            posts = posts,
        )
    }

    fun getPostDetail(postId: Long, currentUserId: Long): PostDetailResponse {
        val post = postRepository.findByIdOrNull(postId)
            ?: throw BusinessException(ErrorCode.POST_NOT_FOUND)

        val author = userRepository.findById(post.authorId).orElse(null)
        val recipeTag = post.recipeId?.let { id ->
            recipeRepository.findByIdOrNull(id)?.let { RecipeTagDto(recipeId = id, recipeName = it.name) }
        }
        val isLiked = postLikeRepository.existsByPostIdAndUserId(postId, currentUserId)

        val commentsPage = postCommentService.getComments(
            postId = postId,
            currentUserId = currentUserId,
            pageable = PageRequest.of(0, 20),
        )

        return PostDetailResponse(
            post = PostDetailPostResponse(
                id = post.id,
                type = post.type,
                author = AuthorDto(id = author?.id ?: post.authorId, username = author?.username ?: ""),
                imageUrls = post.images.map { it.imageUrl },
                caption = post.caption,
                recipeTag = recipeTag,
                rating = post.arRating,
                likeCount = post.likeCount,
                commentCount = post.commentCount,
                isLiked = isLiked,
                isArGenerated = post.isArGenerated,
                isMyPost = post.authorId == currentUserId,
                createdAt = post.createdAt,
            ),
            comments = commentsPage,
        )
    }

    fun createPost(userId: Long, request: CreatePostRequest, images: List<MultipartFile>): PostCreateResponse {
        if (!request.caption.isNullOrEmpty() && request.caption.length > MAX_CAPTION_LENGTH) {
            throw BusinessException(ErrorCode.POST_CAPTION_TOO_LONG)
        }
        if (request.type == PostType.PHOTO && images.isEmpty()) {
            throw BusinessException(ErrorCode.POST_PHOTO_REQUIRED)
        }
        if (images.size > MAX_IMAGE_COUNT) {
            throw BusinessException(ErrorCode.POST_IMAGE_COUNT_EXCEEDED)
        }
        if (request.recipeId != null && !recipeRepository.existsById(request.recipeId)) {
            throw BusinessException(ErrorCode.RECIPE_NOT_FOUND)
        }

        images.forEach { imageUploadValidator.validate(it) }

        val uploadedImageUrls = mutableListOf<String>()

        return try {
            images.forEach { file ->
                uploadedImageUrls += storageService.upload(file, POSTS_DIRECTORY)
            }

            transactionTemplate.execute {
                val post = postRepository.save(
                    Post(
                        authorId = userId,
                        recipeId = request.recipeId,
                        type = request.type,
                        caption = request.caption,
                    ),
                )

                uploadedImageUrls.forEachIndexed { index, imageUrl ->
                    post.images.add(PostImage(post = post, imageUrl = imageUrl, displayOrder = index))
                }

                PostCreateResponse(
                    id = post.id,
                    type = post.type,
                    imageUrls = post.images.map { it.imageUrl },
                    caption = post.caption,
                    createdAt = post.createdAt,
                )
            } ?: throw IllegalStateException("Transaction completed without creating a post")
        } catch (ex: Exception) {
            uploadedImageUrls.forEach(::deleteImageQuietly)
            throw ex
        }
    }

    fun deletePost(postId: Long, userId: Long) {
        val imageUrls = transactionTemplate.execute {
            val post = postRepository.findByIdOrNull(postId)
                ?: throw BusinessException(ErrorCode.POST_NOT_FOUND)
            if (post.isArGenerated) throw BusinessException(ErrorCode.POST_AR_GENERATED_NOT_DELETABLE)
            if (post.authorId != userId) throw BusinessException(ErrorCode.POST_AUTHOR_MISMATCH)

            val urls = post.images.map { it.imageUrl }
            postLikeRepository.deleteAllByPostId(postId)
            postCommentRepository.deleteAllByPostId(postId)
            postRepository.delete(post)
            urls
        } ?: throw IllegalStateException("Transaction completed without deleting a post")

        imageUrls.forEach(::deleteImageQuietly)
    }

    private fun buildFeedPageResponse(
        posts: List<Post>,
        currentUserId: Long,
        pageNum: Int,
        pageSize: Int,
        totalElements: Long,
        totalPages: Int,
        last: Boolean,
    ): FeedPageResponse {
        if (posts.isEmpty()) {
            return FeedPageResponse(emptyList(), pageNum, pageSize, totalElements, totalPages, last)
        }

        val authorIds = posts.map { it.authorId }.toSet()
        val userMap = userRepository.findAllById(authorIds).associateBy { it.id }

        val recipeIds = posts.mapNotNull { it.recipeId }.toSet()
        val recipeMap = if (recipeIds.isNotEmpty()) {
            recipeRepository.findAllById(recipeIds).associateBy { it.id }
        } else {
            emptyMap()
        }

        val postIds = posts.map { it.id }
        val likedPostIds = postLikeRepository.findAllByUserIdAndPostIdIn(currentUserId, postIds)
            .map { it.postId }
            .toSet()

        val content = posts.map { post ->
            val author = userMap[post.authorId]
            val recipe = post.recipeId?.let { recipeMap[it] }
            PostSummaryResponse(
                id = post.id,
                type = post.type,
                author = AuthorDto(id = author?.id ?: post.authorId, username = author?.username ?: ""),
                imageUrls = post.images.map { it.imageUrl },
                caption = post.caption,
                recipeTag = recipe?.let { RecipeTagDto(recipeId = it.id, recipeName = it.name) },
                rating = post.arRating,
                likeCount = post.likeCount,
                commentCount = post.commentCount,
                isLiked = post.id in likedPostIds,
                isArGenerated = post.isArGenerated,
                createdAt = post.createdAt,
            )
        }

        return FeedPageResponse(content, pageNum, pageSize, totalElements, totalPages, last)
    }

    private fun deleteImageQuietly(imageUrl: String) {
        runCatching { storageService.delete(imageUrl) }
            .onFailure { ex -> log.warn("Failed to delete community image: {}", imageUrl, ex) }
    }
}
