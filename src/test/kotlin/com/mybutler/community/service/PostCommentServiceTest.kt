package com.mybutler.community.service

import com.mybutler.auth.entity.User
import com.mybutler.auth.repository.UserRepository
import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.community.entity.Post
import com.mybutler.community.entity.PostComment
import com.mybutler.community.entity.PostType
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
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class PostCommentServiceTest {

    @Mock lateinit var postRepository: PostRepository
    @Mock lateinit var postCommentRepository: PostCommentRepository
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var eventPublisher: ApplicationEventPublisher

    private lateinit var postCommentService: PostCommentService

    @BeforeEach
    fun setUp() {
        postCommentService = PostCommentService(
            postRepository = postRepository,
            postCommentRepository = postCommentRepository,
            userRepository = userRepository,
            eventPublisher = eventPublisher,
        )
    }

    // ── addComment ───────────────────────────────────────────────────────

    @Test
    fun `addComment - 성공 시 댓글 반환 및 commentCount 증가`() {
        val post = post(id = 1L, commentCount = 2)
        val saved = PostComment(id = 100L, postId = 1L, authorId = 10L, content = "좋아요")
        whenever(postRepository.findByIdForUpdate(1L)).thenReturn(post)
        whenever(postCommentRepository.save(any())).thenReturn(saved)

        val result = postCommentService.addComment(postId = 1L, userId = 10L, content = "좋아요")

        assertThat(result.id).isEqualTo(100L)
        assertThat(result.content).isEqualTo("좋아요")
        assertThat(post.commentCount).isEqualTo(3)
    }

    @Test
    fun `addComment - 빈 내용이면 COMMENT_CONTENT_REQUIRED 예외`() {
        val ex = assertThrows<BusinessException> {
            postCommentService.addComment(postId = 1L, userId = 10L, content = "  ")
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.COMMENT_CONTENT_REQUIRED)
        verify(postRepository, never()).findByIdForUpdate(any())
    }

    @Test
    fun `addComment - 게시물이 없으면 POST_NOT_FOUND 예외`() {
        whenever(postRepository.findByIdForUpdate(1L)).thenReturn(null)

        val ex = assertThrows<BusinessException> {
            postCommentService.addComment(postId = 1L, userId = 10L, content = "댓글")
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.POST_NOT_FOUND)
    }

    // ── deleteComment ────────────────────────────────────────────────────

    @Test
    fun `deleteComment - 성공 시 댓글 및 대댓글 삭제하고 commentCount 감소`() {
        val post = post(id = 1L, commentCount = 3)
        val comment = PostComment(id = 10L, postId = 1L, authorId = 2L, content = "top")
        whenever(postCommentRepository.findById(10L)).thenReturn(Optional.of(comment))
        whenever(postRepository.findByIdForUpdate(1L)).thenReturn(post)
        whenever(postCommentRepository.countByParentCommentId(10L)).thenReturn(1L)

        postCommentService.deleteComment(postId = 1L, commentId = 10L, userId = 2L)

        verify(postCommentRepository).deleteAllByParentCommentId(10L)
        verify(postCommentRepository).delete(comment)
        assertThat(post.commentCount).isEqualTo(1)
    }

    @Test
    fun `deleteComment - 작성자가 아니면 COMMENT_AUTHOR_MISMATCH 예외`() {
        val comment = PostComment(id = 10L, postId = 1L, authorId = 2L, content = "top")
        whenever(postCommentRepository.findById(10L)).thenReturn(Optional.of(comment))

        val ex = assertThrows<BusinessException> {
            postCommentService.deleteComment(postId = 1L, commentId = 10L, userId = 99L)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.COMMENT_AUTHOR_MISMATCH)
        verify(postRepository, never()).findByIdForUpdate(any())
    }

    @Test
    fun `deleteComment - 대댓글 id로 삭제하면 COMMENT_INVALID_TARGET 예외`() {
        val reply = PostComment(id = 11L, postId = 1L, parentCommentId = 10L, authorId = 2L, content = "reply")
        whenever(postCommentRepository.findById(11L)).thenReturn(Optional.of(reply))

        val exception = assertThrows<BusinessException> {
            postCommentService.deleteComment(postId = 1L, commentId = 11L, userId = 2L)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.COMMENT_INVALID_TARGET)
        verify(postRepository, never()).findByIdForUpdate(any())
    }

    @Test
    fun `deleteComment - 댓글이 없으면 COMMENT_NOT_FOUND 예외`() {
        whenever(postCommentRepository.findById(10L)).thenReturn(Optional.empty())

        val ex = assertThrows<BusinessException> {
            postCommentService.deleteComment(postId = 1L, commentId = 10L, userId = 2L)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.COMMENT_NOT_FOUND)
    }

    // ── addReply ─────────────────────────────────────────────────────────

    @Test
    fun `addReply - 성공 시 대댓글 반환 및 commentCount 증가`() {
        val post = post(id = 1L, commentCount = 1)
        val parent = PostComment(id = 10L, postId = 1L, authorId = 2L, content = "parent")
        val saved = PostComment(id = 20L, postId = 1L, parentCommentId = 10L, authorId = 3L, content = "reply")
        whenever(postCommentRepository.findById(10L)).thenReturn(Optional.of(parent))
        whenever(postRepository.findByIdForUpdate(1L)).thenReturn(post)
        whenever(postCommentRepository.save(any())).thenReturn(saved)

        val result = postCommentService.addReply(postId = 1L, parentCommentId = 10L, userId = 3L, content = "reply")

        assertThat(result.id).isEqualTo(20L)
        assertThat(post.commentCount).isEqualTo(2)
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
    fun `addReply - 부모 댓글의 postId가 다르면 COMMENT_INVALID_TARGET 예외`() {
        val parent = PostComment(id = 10L, postId = 99L, authorId = 2L, content = "parent")
        whenever(postCommentRepository.findById(10L)).thenReturn(Optional.of(parent))

        val ex = assertThrows<BusinessException> {
            postCommentService.addReply(postId = 1L, parentCommentId = 10L, userId = 3L, content = "reply")
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.COMMENT_INVALID_TARGET)
    }

    // ── deleteReply ──────────────────────────────────────────────────────

    @Test
    fun `deleteReply - 성공 시 대댓글 삭제 및 commentCount 감소`() {
        val post = post(id = 1L, commentCount = 2)
        val reply = PostComment(id = 11L, postId = 1L, parentCommentId = 10L, authorId = 2L, content = "reply")
        whenever(postCommentRepository.findById(11L)).thenReturn(Optional.of(reply))
        whenever(postRepository.findByIdForUpdate(1L)).thenReturn(post)

        postCommentService.deleteReply(postId = 1L, commentId = 10L, replyId = 11L, userId = 2L)

        verify(postCommentRepository).delete(reply)
        assertThat(post.commentCount).isEqualTo(1)
    }

    @Test
    fun `deleteReply - 작성자가 아니면 COMMENT_AUTHOR_MISMATCH 예외`() {
        val reply = PostComment(id = 11L, postId = 1L, parentCommentId = 10L, authorId = 2L, content = "reply")
        whenever(postCommentRepository.findById(11L)).thenReturn(Optional.of(reply))

        val ex = assertThrows<BusinessException> {
            postCommentService.deleteReply(postId = 1L, commentId = 10L, replyId = 11L, userId = 99L)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.COMMENT_AUTHOR_MISMATCH)
    }

    @Test
    fun `deleteReply - 부모 댓글 경로가 다르면 COMMENT_INVALID_TARGET 예외`() {
        val reply = PostComment(id = 11L, postId = 1L, parentCommentId = 99L, authorId = 2L, content = "reply")
        whenever(postCommentRepository.findById(11L)).thenReturn(Optional.of(reply))

        val exception = assertThrows<BusinessException> {
            postCommentService.deleteReply(postId = 1L, commentId = 10L, replyId = 11L, userId = 2L)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.COMMENT_INVALID_TARGET)
        verify(postRepository, never()).findByIdForUpdate(any())
    }

    @Test
    fun `deleteReply - commentCount가 0이면 음수로 내려가지 않는다`() {
        val post = post(id = 1L, commentCount = 0)
        val reply = PostComment(id = 11L, postId = 1L, parentCommentId = 10L, authorId = 2L, content = "reply")
        whenever(postCommentRepository.findById(11L)).thenReturn(Optional.of(reply))
        whenever(postRepository.findByIdForUpdate(1L)).thenReturn(post)

        postCommentService.deleteReply(postId = 1L, commentId = 10L, replyId = 11L, userId = 2L)

        assertThat(post.commentCount).isEqualTo(0)
    }

    // ── getComments ──────────────────────────────────────────────────────

    @Test
    fun `getComments - 댓글 목록과 대댓글 포함하여 반환`() {
        val pageable = PageRequest.of(0, 20)
        val comment = PostComment(id = 10L, postId = 1L, authorId = 2L, content = "top comment")
        val reply = PostComment(id = 20L, postId = 1L, parentCommentId = 10L, authorId = 3L, content = "reply")
        val user2 = User(id = 2L, email = "a@test.com", username = "userA", password = "pw")
        val user3 = User(id = 3L, email = "b@test.com", username = "userB", password = "pw")

        whenever(postCommentRepository.findByPostIdAndParentCommentIdIsNullOrderByCreatedAtAsc(1L, pageable))
            .thenReturn(PageImpl(listOf(comment), pageable, 1L))
        whenever(postCommentRepository.findAllByParentCommentIdInOrderByParentCommentIdAscCreatedAtAsc(listOf(10L)))
            .thenReturn(listOf(reply))
        whenever(userRepository.findAllById(setOf(2L, 3L))).thenReturn(listOf(user2, user3))

        val result = postCommentService.getComments(postId = 1L, currentUserId = 2L, pageable = pageable)

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].replies).hasSize(1)
        assertThat(result.content[0].isMyComment).isTrue()
        assertThat(result.content[0].replies[0].isMyComment).isFalse()
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private fun post(id: Long, commentCount: Int = 0) =
        Post(id = id, authorId = 1L, type = PostType.TEXT, commentCount = commentCount)
}
