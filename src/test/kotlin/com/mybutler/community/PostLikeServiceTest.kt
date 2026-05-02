package com.mybutler.community

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.community.entity.Post
import com.mybutler.community.entity.PostLike
import com.mybutler.community.repository.PostLikeRepository
import com.mybutler.community.repository.PostRepository
import com.mybutler.community.service.PostLikeService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class PostLikeServiceTest {

    @Mock lateinit var postRepository: PostRepository
    @Mock lateinit var postLikeRepository: PostLikeRepository

    private lateinit var postLikeService: PostLikeService

    @BeforeEach
    fun setUp() {
        postLikeService = PostLikeService(postRepository, postLikeRepository)
    }

    @Test
    fun `toggleLike - 좋아요 없으면 추가`() {
        val post = post(id = 1L, likeCount = 0)
        given(postRepository.findById(1L)).willReturn(Optional.of(post))
        given(postLikeRepository.existsByPostIdAndUserId(1L, 10L)).willReturn(false)
        given(postLikeRepository.save(any<PostLike>())).willReturn(PostLike(postId = 1L, userId = 10L))

        val result = postLikeService.toggleLike(1L, 10L)

        assertThat(result.liked).isTrue()
        assertThat(result.likeCount).isEqualTo(1)
        assertThat(post.likeCount).isEqualTo(1)
    }

    @Test
    fun `toggleLike - 좋아요 있으면 취소`() {
        val post = post(id = 1L, likeCount = 3)
        given(postRepository.findById(1L)).willReturn(Optional.of(post))
        given(postLikeRepository.existsByPostIdAndUserId(1L, 10L)).willReturn(true)

        val result = postLikeService.toggleLike(1L, 10L)

        assertThat(result.liked).isFalse()
        assertThat(result.likeCount).isEqualTo(2)
        verify(postLikeRepository).deleteByPostIdAndUserId(1L, 10L)
        verify(postLikeRepository, never()).save(any<PostLike>())
    }

    @Test
    fun `toggleLike - 게시글 없으면 POST_NOT_FOUND`() {
        given(postRepository.findById(999L)).willReturn(Optional.empty())

        val ex = assertThrows<BusinessException> { postLikeService.toggleLike(999L, 10L) }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.POST_NOT_FOUND)
    }

    @Test
    fun `getLikeStatus - 좋아요 상태 반환`() {
        val post = post(id = 1L, likeCount = 5)
        given(postRepository.findById(1L)).willReturn(Optional.of(post))
        given(postLikeRepository.existsByPostIdAndUserId(1L, 10L)).willReturn(true)

        val result = postLikeService.getLikeStatus(1L, 10L)

        assertThat(result.liked).isTrue()
        assertThat(result.likeCount).isEqualTo(5)
    }

    private fun post(id: Long = 1L, likeCount: Int = 0) =
        Post(id = id, userId = 1L, title = "t", content = "c", likeCount = likeCount)
}
