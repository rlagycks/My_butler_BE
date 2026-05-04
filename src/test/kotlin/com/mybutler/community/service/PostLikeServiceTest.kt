package com.mybutler.community.service

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.community.entity.Post
import com.mybutler.community.entity.PostLike
import com.mybutler.community.entity.PostType
import com.mybutler.community.repository.PostLikeRepository
import com.mybutler.community.repository.PostRepository
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

@ExtendWith(MockitoExtension::class)
class PostLikeServiceTest {

    @Mock lateinit var postRepository: PostRepository
    @Mock lateinit var postLikeRepository: PostLikeRepository

    private lateinit var postLikeService: PostLikeService

    @BeforeEach
    fun setUp() {
        postLikeService = PostLikeService(
            postRepository = postRepository,
            postLikeRepository = postLikeRepository,
        )
    }

    // ── addLike ──────────────────────────────────────────────────────────

    @Test
    fun `addLike - 성공 시 likeCount 증가 및 isLiked true 반환`() {
        val post = post(id = 1L, likeCount = 5)
        whenever(postLikeRepository.existsByPostIdAndUserId(1L, 10L)).thenReturn(false)
        whenever(postRepository.findByIdForUpdate(1L)).thenReturn(post)
        whenever(postLikeRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = postLikeService.addLike(postId = 1L, userId = 10L)

        assertThat(result.likeCount).isEqualTo(6)
        assertThat(result.isLiked).isTrue()
        assertThat(result.postId).isEqualTo(1L)
    }

    @Test
    fun `addLike - 이미 좋아요한 경우 POST_LIKE_ALREADY_EXISTS 예외`() {
        whenever(postLikeRepository.existsByPostIdAndUserId(1L, 10L)).thenReturn(true)

        val ex = assertThrows<BusinessException> {
            postLikeService.addLike(postId = 1L, userId = 10L)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.POST_LIKE_ALREADY_EXISTS)
        verify(postRepository, never()).findByIdForUpdate(any())
    }

    @Test
    fun `addLike - 게시물이 없으면 POST_NOT_FOUND 예외`() {
        whenever(postLikeRepository.existsByPostIdAndUserId(1L, 10L)).thenReturn(false)
        whenever(postRepository.findByIdForUpdate(1L)).thenReturn(null)

        val ex = assertThrows<BusinessException> {
            postLikeService.addLike(postId = 1L, userId = 10L)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.POST_NOT_FOUND)
        verify(postLikeRepository, never()).save(any())
    }

    // ── removeLike ───────────────────────────────────────────────────────

    @Test
    fun `removeLike - 성공 시 likeCount 감소 및 isLiked false 반환`() {
        val post = post(id = 1L, likeCount = 3)
        val like = PostLike(id = 5L, postId = 1L, userId = 10L)
        whenever(postLikeRepository.findByPostIdAndUserId(1L, 10L)).thenReturn(like)
        whenever(postRepository.findByIdForUpdate(1L)).thenReturn(post)

        val result = postLikeService.removeLike(postId = 1L, userId = 10L)

        assertThat(result.likeCount).isEqualTo(2)
        assertThat(result.isLiked).isFalse()
        assertThat(result.postId).isEqualTo(1L)
    }

    @Test
    fun `removeLike - likeCount가 0이면 음수로 내려가지 않는다`() {
        val post = post(id = 1L, likeCount = 0)
        val like = PostLike(id = 5L, postId = 1L, userId = 10L)
        whenever(postLikeRepository.findByPostIdAndUserId(1L, 10L)).thenReturn(like)
        whenever(postRepository.findByIdForUpdate(1L)).thenReturn(post)

        val result = postLikeService.removeLike(postId = 1L, userId = 10L)

        assertThat(result.likeCount).isEqualTo(0)
    }

    @Test
    fun `removeLike - 좋아요 레코드가 없으면 POST_LIKE_NOT_FOUND 예외`() {
        whenever(postLikeRepository.findByPostIdAndUserId(1L, 10L)).thenReturn(null)

        val ex = assertThrows<BusinessException> {
            postLikeService.removeLike(postId = 1L, userId = 10L)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.POST_LIKE_NOT_FOUND)
        verify(postRepository, never()).findByIdForUpdate(any())
    }

    @Test
    fun `removeLike - 게시물이 없으면 POST_NOT_FOUND 예외`() {
        val like = PostLike(id = 5L, postId = 1L, userId = 10L)
        whenever(postLikeRepository.findByPostIdAndUserId(1L, 10L)).thenReturn(like)
        whenever(postRepository.findByIdForUpdate(1L)).thenReturn(null)

        val ex = assertThrows<BusinessException> {
            postLikeService.removeLike(postId = 1L, userId = 10L)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.POST_NOT_FOUND)
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private fun post(id: Long, likeCount: Int = 0) = Post(
        id = id,
        authorId = 1L,
        type = PostType.TEXT,
        likeCount = likeCount,
    )
}
