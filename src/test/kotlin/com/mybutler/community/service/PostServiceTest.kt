package com.mybutler.community.service

import com.mybutler.auth.entity.User
import com.mybutler.auth.repository.UserRepository
import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.common.storage.StorageService
import com.mybutler.community.dto.CommentPageResponse
import com.mybutler.community.dto.CreatePostRequest
import com.mybutler.community.entity.Post
import com.mybutler.community.entity.PostType
import com.mybutler.community.repository.PostCommentRepository
import com.mybutler.community.repository.PostLikeRepository
import com.mybutler.community.repository.PostRepository
import com.mybutler.recipe.entity.Recipe
import com.mybutler.recipe.entity.RecipeCategory
import com.mybutler.recipe.repository.RecipeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.mock.web.MockMultipartFile
import org.mockito.Mockito.mock
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class PostServiceTest {

    @Mock lateinit var postRepository: PostRepository
    @Mock lateinit var postLikeRepository: PostLikeRepository
    @Mock lateinit var postCommentRepository: PostCommentRepository
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var recipeRepository: RecipeRepository
    @Mock lateinit var storageService: StorageService
    @Mock lateinit var postCommentService: PostCommentService
    @Mock lateinit var transactionTemplate: TransactionTemplate

    private lateinit var postService: PostService

    @BeforeEach
    fun setUp() {
        postService = PostService(
            postRepository = postRepository,
            postLikeRepository = postLikeRepository,
            postCommentRepository = postCommentRepository,
            userRepository = userRepository,
            recipeRepository = recipeRepository,
            storageService = storageService,
            postCommentService = postCommentService,
            transactionTemplate = transactionTemplate,
        )
    }

    // ── getPostFeed ──────────────────────────────────────────────────────

    @Test
    fun `getPostFeed - LATEST 정렬로 피드 반환`() {
        val pageable = PageRequest.of(0, 20)
        val posts = listOf(post(id = 1L, authorId = 10L))
        val page = PageImpl(posts, pageable, 1L)
        whenever(postRepository.findAllForFeedLatest(pageable)).thenReturn(page)
        whenever(userRepository.findAllById(setOf(10L))).thenReturn(listOf(user(id = 10L)))
        whenever(postLikeRepository.findAllByUserIdAndPostIdIn(10L, listOf(1L))).thenReturn(emptyList())

        val result = postService.getPostFeed(currentUserId = 10L, sort = "LATEST", pageable = pageable)

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].id).isEqualTo(1L)
    }

    @Test
    fun `getPostFeed - POPULAR 정렬로 피드 반환`() {
        val pageable = PageRequest.of(0, 20)
        val posts = listOf(post(id = 2L, authorId = 10L, likeCount = 5))
        val page = PageImpl(posts, pageable, 1L)
        whenever(postRepository.findAllForFeedPopular(pageable)).thenReturn(page)
        whenever(userRepository.findAllById(setOf(10L))).thenReturn(listOf(user(id = 10L)))
        whenever(postLikeRepository.findAllByUserIdAndPostIdIn(10L, listOf(2L))).thenReturn(emptyList())

        val result = postService.getPostFeed(currentUserId = 10L, sort = "POPULAR", pageable = pageable)

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].likeCount).isEqualTo(5)
    }

    @Test
    fun `getPostFeed - 게시물이 없으면 빈 목록 반환`() {
        val pageable = PageRequest.of(0, 20)
        whenever(postRepository.findAllForFeedLatest(pageable)).thenReturn(PageImpl(emptyList(), pageable, 0L))

        val result = postService.getPostFeed(currentUserId = 10L, sort = "LATEST", pageable = pageable)

        assertThat(result.content).isEmpty()
        assertThat(result.totalElements).isEqualTo(0)
    }

    @Test
    fun `getPostFeed - 좋아요한 게시물은 isLiked true`() {
        val pageable = PageRequest.of(0, 20)
        val post = post(id = 1L, authorId = 10L)
        val like = com.mybutler.community.entity.PostLike(postId = 1L, userId = 10L)
        whenever(postRepository.findAllForFeedLatest(pageable)).thenReturn(PageImpl(listOf(post), pageable, 1L))
        whenever(userRepository.findAllById(setOf(10L))).thenReturn(listOf(user(id = 10L)))
        whenever(postLikeRepository.findAllByUserIdAndPostIdIn(10L, listOf(1L))).thenReturn(listOf(like))

        val result = postService.getPostFeed(currentUserId = 10L, sort = "LATEST", pageable = pageable)

        assertThat(result.content[0].isLiked).isTrue()
    }

    // ── getMyPosts ───────────────────────────────────────────────────────

    @Test
    fun `getMyPosts - 내 게시물 목록 반환`() {
        val pageable = PageRequest.of(0, 9)
        val posts = listOf(post(id = 1L, authorId = 10L), post(id = 2L, authorId = 10L))
        whenever(postRepository.findAllByAuthorId(10L, pageable)).thenReturn(PageImpl(posts, pageable, 2L))
        whenever(userRepository.findAllById(setOf(10L))).thenReturn(listOf(user(id = 10L)))
        whenever(postLikeRepository.findAllByUserIdAndPostIdIn(10L, listOf(1L, 2L))).thenReturn(emptyList())

        val result = postService.getMyPosts(currentUserId = 10L, pageable = pageable)

        assertThat(result.content).hasSize(2)
        assertThat(result.totalElements).isEqualTo(2)
    }

    // ── getPostDetail ────────────────────────────────────────────────────

    @Test
    fun `getPostDetail - 게시물 상세 반환`() {
        val post = post(id = 1L, authorId = 10L)
        val emptyComments = CommentPageResponse(emptyList(), 0, 20, 0L, 0, true)
        whenever(postRepository.findById(1L)).thenReturn(Optional.of(post))
        whenever(userRepository.findById(10L)).thenReturn(Optional.of(user(id = 10L)))
        whenever(postLikeRepository.existsByPostIdAndUserId(1L, 10L)).thenReturn(false)
        whenever(postCommentService.getComments(any(), any(), any())).thenReturn(emptyComments)

        val result = postService.getPostDetail(postId = 1L, currentUserId = 10L)

        assertThat(result.post.id).isEqualTo(1L)
        assertThat(result.post.isMyPost).isTrue()
        assertThat(result.post.isLiked).isFalse()
    }

    @Test
    fun `getPostDetail - 존재하지 않는 게시물이면 POST_NOT_FOUND 예외`() {
        whenever(postRepository.findById(99L)).thenReturn(Optional.empty())

        val ex = assertThrows<BusinessException> {
            postService.getPostDetail(postId = 99L, currentUserId = 10L)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.POST_NOT_FOUND)
    }

    @Test
    fun `getPostDetail - 레시피 태그가 있으면 recipeTag 포함`() {
        val recipe = recipe(id = 5L, name = "Mojito")
        val post = post(id = 1L, authorId = 10L, recipeId = 5L)
        val emptyComments = CommentPageResponse(emptyList(), 0, 20, 0L, 0, true)
        whenever(postRepository.findById(1L)).thenReturn(Optional.of(post))
        whenever(userRepository.findById(10L)).thenReturn(Optional.of(user(id = 10L)))
        whenever(recipeRepository.findById(5L)).thenReturn(Optional.of(recipe))
        whenever(postLikeRepository.existsByPostIdAndUserId(1L, 10L)).thenReturn(false)
        whenever(postCommentService.getComments(any(), any(), any())).thenReturn(emptyComments)

        val result = postService.getPostDetail(postId = 1L, currentUserId = 10L)

        assertThat(result.post.recipeTag).isNotNull
        assertThat(result.post.recipeTag!!.recipeName).isEqualTo("Mojito")
    }

    // ── createPost ───────────────────────────────────────────────────────

    @Test
    fun `createPost - TEXT 타입 이미지 없이 성공`() {
        val request = CreatePostRequest(type = PostType.TEXT, caption = "hello")
        val savedPost = post(id = 1L, authorId = 10L, type = PostType.TEXT)
        whenever(transactionTemplate.execute(any<TransactionCallback<*>>())).thenAnswer { invocation ->
            val callback = invocation.getArgument<TransactionCallback<*>>(0)
            callback.doInTransaction(mock(TransactionStatus::class.java))
        }
        whenever(postRepository.save(any())).thenReturn(savedPost)

        val result = postService.createPost(userId = 10L, request = request, images = emptyList())

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.type).isEqualTo(PostType.TEXT)
    }

    @Test
    fun `createPost - caption이 2000자를 초과하면 POST_CAPTION_TOO_LONG 예외`() {
        val request = CreatePostRequest(type = PostType.TEXT, caption = "A".repeat(2001))

        val ex = assertThrows<BusinessException> {
            postService.createPost(userId = 10L, request = request, images = emptyList())
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.POST_CAPTION_TOO_LONG)
        verify(transactionTemplate, never()).execute(any<TransactionCallback<*>>())
    }

    @Test
    fun `createPost - PHOTO 타입에 이미지가 없으면 POST_PHOTO_REQUIRED 예외`() {
        val request = CreatePostRequest(type = PostType.PHOTO)

        val ex = assertThrows<BusinessException> {
            postService.createPost(userId = 10L, request = request, images = emptyList())
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.POST_PHOTO_REQUIRED)
    }

    @Test
    fun `createPost - 이미지가 10장을 초과하면 POST_IMAGE_COUNT_EXCEEDED 예외`() {
        val images = (1..11).map { MockMultipartFile("img$it", ByteArray(10)) }
        val request = CreatePostRequest(type = PostType.PHOTO)

        val ex = assertThrows<BusinessException> {
            postService.createPost(userId = 10L, request = request, images = images)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.POST_IMAGE_COUNT_EXCEEDED)
    }

    @Test
    fun `createPost - 존재하지 않는 레시피 태그이면 RECIPE_NOT_FOUND 예외`() {
        val request = CreatePostRequest(type = PostType.TEXT, recipeId = 999L)
        whenever(recipeRepository.existsById(999L)).thenReturn(false)

        val ex = assertThrows<BusinessException> {
            postService.createPost(userId = 10L, request = request, images = emptyList())
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.RECIPE_NOT_FOUND)
    }

    @Test
    fun `createPost - 이미지 업로드 실패 시 업로드된 이미지 롤백 후 예외 전파`() {
        val images = listOf(
            MockMultipartFile("img1", ByteArray(10)),
            MockMultipartFile("img2", ByteArray(10)),
        )
        val request = CreatePostRequest(type = PostType.PHOTO)
        whenever(storageService.upload(images[0], "posts")).thenReturn("url1")
        whenever(storageService.upload(images[1], "posts")).thenThrow(RuntimeException("S3 error"))

        assertThrows<RuntimeException> {
            postService.createPost(userId = 10L, request = request, images = images)
        }

        verify(storageService).delete("url1")
    }

    // ── deletePost ───────────────────────────────────────────────────────

    @Test
    fun `deletePost - 성공 시 게시물 및 연관 데이터 삭제`() {
        val post = post(id = 1L, authorId = 10L)
        whenever(transactionTemplate.execute(any<TransactionCallback<*>>())).thenAnswer { invocation ->
            val callback = invocation.getArgument<TransactionCallback<*>>(0)
            callback.doInTransaction(mock(TransactionStatus::class.java))
        }
        whenever(postRepository.findById(1L)).thenReturn(Optional.of(post))

        postService.deletePost(postId = 1L, userId = 10L)

        verify(postRepository).delete(post)
        verify(postLikeRepository).deleteAllByPostId(1L)
        verify(postCommentRepository).deleteAllByPostId(1L)
    }

    @Test
    fun `deletePost - 존재하지 않는 게시물이면 POST_NOT_FOUND 예외`() {
        whenever(transactionTemplate.execute(any<TransactionCallback<*>>())).thenAnswer { invocation ->
            val callback = invocation.getArgument<TransactionCallback<*>>(0)
            callback.doInTransaction(mock(TransactionStatus::class.java))
        }
        whenever(postRepository.findById(99L)).thenReturn(Optional.empty())

        val ex = assertThrows<BusinessException> {
            postService.deletePost(postId = 99L, userId = 10L)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.POST_NOT_FOUND)
    }

    @Test
    fun `deletePost - 작성자가 아니면 POST_AUTHOR_MISMATCH 예외`() {
        val post = post(id = 1L, authorId = 10L)
        whenever(transactionTemplate.execute(any<TransactionCallback<*>>())).thenAnswer { invocation ->
            val callback = invocation.getArgument<TransactionCallback<*>>(0)
            callback.doInTransaction(mock(TransactionStatus::class.java))
        }
        whenever(postRepository.findById(1L)).thenReturn(Optional.of(post))

        val ex = assertThrows<BusinessException> {
            postService.deletePost(postId = 1L, userId = 99L)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.POST_AUTHOR_MISMATCH)
        verify(postRepository, never()).delete(any())
    }

    @Test
    fun `deletePost - AR 생성 게시물은 POST_AR_GENERATED_NOT_DELETABLE 예외`() {
        val post = Post(id = 1L, authorId = 10L, type = PostType.PHOTO, isArGenerated = true)
        whenever(transactionTemplate.execute(any<TransactionCallback<*>>())).thenAnswer { invocation ->
            val callback = invocation.getArgument<TransactionCallback<*>>(0)
            callback.doInTransaction(mock(TransactionStatus::class.java))
        }
        whenever(postRepository.findById(1L)).thenReturn(Optional.of(post))

        val ex = assertThrows<BusinessException> {
            postService.deletePost(postId = 1L, userId = 10L)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.POST_AR_GENERATED_NOT_DELETABLE)
        verify(postRepository, never()).delete(any())
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private fun post(
        id: Long,
        authorId: Long,
        type: PostType = PostType.TEXT,
        likeCount: Int = 0,
        recipeId: Long? = null,
    ) = Post(id = id, authorId = authorId, type = type, likeCount = likeCount, recipeId = recipeId)

    private fun user(id: Long) = User(id = id, email = "user$id@test.com", username = "user$id", password = "pw")

    private fun recipe(id: Long, name: String) = Recipe(
        id = id,
        name = name,
        category = RecipeCategory.WHISKEY,
    )
}
