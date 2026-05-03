package com.mybutler.community.service

import com.mybutler.auth.repository.UserRepository
import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.community.entity.PostComment
import com.mybutler.community.repository.PostCommentRepository
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
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class PostCommentServiceTest {

    @Mock
    lateinit var postRepository: PostRepository

    @Mock
    lateinit var postCommentRepository: PostCommentRepository

    @Mock
    lateinit var userRepository: UserRepository

    private lateinit var postCommentService: PostCommentService

    @BeforeEach
    fun setUp() {
        postCommentService = PostCommentService(
            postRepository = postRepository,
            postCommentRepository = postCommentRepository,
            userRepository = userRepository,
        )
    }

    @Test
    fun `deleteComment - 대댓글 id로 삭제하면 예외가 발생한다`() {
        val reply = PostComment(id = 11L, postId = 1L, parentCommentId = 10L, authorId = 2L, content = "reply")
        whenever(postCommentRepository.findById(11L)).thenReturn(Optional.of(reply))

        val exception = assertThrows<BusinessException> {
            postCommentService.deleteComment(postId = 1L, commentId = 11L, userId = 2L)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.COMMENT_INVALID_TARGET)
        verify(postRepository, never()).findByIdForUpdate(any())
    }

    @Test
    fun `addReply - 대댓글에는 다시 답글을 달 수 없다`() {
        val reply = PostComment(id = 11L, postId = 1L, parentCommentId = 10L, authorId = 2L, content = "reply")
        whenever(postCommentRepository.findById(11L)).thenReturn(Optional.of(reply))

        val exception = assertThrows<BusinessException> {
            postCommentService.addReply(postId = 1L, parentCommentId = 11L, userId = 3L, content = "nested reply")
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.COMMENT_REPLY_DEPTH_EXCEEDED)
        verify(postRepository, never()).findByIdForUpdate(any())
    }

    @Test
    fun `deleteReply - 부모 댓글 경로가 다르면 예외가 발생한다`() {
        val reply = PostComment(id = 11L, postId = 1L, parentCommentId = 99L, authorId = 2L, content = "reply")
        whenever(postCommentRepository.findById(11L)).thenReturn(Optional.of(reply))

        val exception = assertThrows<BusinessException> {
            postCommentService.deleteReply(postId = 1L, commentId = 10L, replyId = 11L, userId = 2L)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.COMMENT_INVALID_TARGET)
        verify(postRepository, never()).findByIdForUpdate(any())
    }
}
