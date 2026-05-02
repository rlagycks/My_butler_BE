package com.mybutler.community

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.community.dto.CreateCommentRequest
import com.mybutler.community.dto.UpdateCommentRequest
import com.mybutler.community.entity.Comment
import com.mybutler.community.entity.Post
import com.mybutler.community.repository.CommentRepository
import com.mybutler.community.repository.PostRepository
import com.mybutler.community.service.CommentService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class CommentServiceTest {

    @Mock lateinit var postRepository: PostRepository
    @Mock lateinit var commentRepository: CommentRepository

    private lateinit var commentService: CommentService

    @BeforeEach
    fun setUp() {
        commentService = CommentService(postRepository, commentRepository)
    }

    @Test
    fun `createComment - 최상위 댓글 작성`() {
        val post = post(id = 1L, commentCount = 0)
        val saved = comment(id = 10L, postId = 1L, userId = 5L, content = "hello")
        given(postRepository.findById(1L)).willReturn(Optional.of(post))
        given(commentRepository.save(any<Comment>())).willReturn(saved)

        val result = commentService.createComment(1L, 5L, CreateCommentRequest(content = "hello"))

        assertThat(result.content).isEqualTo("hello")
        assertThat(result.parentCommentId).isNull()
        assertThat(post.commentCount).isEqualTo(1)
    }

    @Test
    fun `createComment - 대댓글 작성`() {
        val post = post(id = 1L)
        val saved = comment(id = 11L, postId = 1L, userId = 5L, parentCommentId = 10L, content = "reply")
        given(postRepository.findById(1L)).willReturn(Optional.of(post))
        given(commentRepository.save(any<Comment>())).willReturn(saved)

        val result = commentService.createComment(1L, 5L, CreateCommentRequest(content = "reply", parentCommentId = 10L))

        assertThat(result.parentCommentId).isEqualTo(10L)
    }

    @Test
    fun `createComment - 게시글 없으면 POST_NOT_FOUND`() {
        given(postRepository.findById(999L)).willReturn(Optional.empty())

        val ex = assertThrows<BusinessException> {
            commentService.createComment(999L, 1L, CreateCommentRequest(content = "x"))
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.POST_NOT_FOUND)
    }

    @Test
    fun `getComments - 최상위 댓글 목록 반환`() {
        val pageable = PageRequest.of(0, 20)
        given(postRepository.existsById(1L)).willReturn(true)
        given(commentRepository.findByPostIdAndParentCommentIdIsNull(1L, pageable)).willReturn(
            PageImpl(listOf(comment(id = 1L, postId = 1L), comment(id = 2L, postId = 1L)), pageable, 2)
        )

        val result = commentService.getComments(1L, pageable)

        assertThat(result.content).hasSize(2)
        assertThat(result.totalElements).isEqualTo(2)
    }

    @Test
    fun `getComments - 게시글 없으면 POST_NOT_FOUND`() {
        given(postRepository.existsById(999L)).willReturn(false)

        val ex = assertThrows<BusinessException> {
            commentService.getComments(999L, PageRequest.of(0, 20))
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.POST_NOT_FOUND)
    }

    @Test
    fun `getReplies - 대댓글 목록 반환`() {
        val pageable = PageRequest.of(0, 20)
        given(commentRepository.existsById(10L)).willReturn(true)
        given(commentRepository.findByParentCommentId(10L, pageable)).willReturn(
            PageImpl(listOf(comment(id = 20L, postId = 1L, parentCommentId = 10L)), pageable, 1)
        )

        val result = commentService.getReplies(10L, pageable)

        assertThat(result.content).hasSize(1)
        assertThat(result.content.first().parentCommentId).isEqualTo(10L)
    }

    @Test
    fun `getReplies - 댓글 없으면 COMMENT_NOT_FOUND`() {
        given(commentRepository.existsById(999L)).willReturn(false)

        val ex = assertThrows<BusinessException> {
            commentService.getReplies(999L, PageRequest.of(0, 20))
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.COMMENT_NOT_FOUND)
    }

    @Test
    fun `updateComment - 작성자가 수정`() {
        val c = comment(id = 1L, postId = 1L, userId = 5L, content = "old")
        given(commentRepository.findById(1L)).willReturn(Optional.of(c))

        val result = commentService.updateComment(1L, 5L, UpdateCommentRequest(content = "new"))

        assertThat(result.content).isEqualTo("new")
        assertThat(c.content).isEqualTo("new")
    }

    @Test
    fun `updateComment - 다른 사용자가 수정 시 COMMENT_AUTHOR_MISMATCH`() {
        val c = comment(id = 1L, postId = 1L, userId = 5L)
        given(commentRepository.findById(1L)).willReturn(Optional.of(c))

        val ex = assertThrows<BusinessException> {
            commentService.updateComment(1L, 99L, UpdateCommentRequest(content = "x"))
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.COMMENT_AUTHOR_MISMATCH)
    }

    @Test
    fun `deleteComment - 작성자가 삭제하면 commentCount 감소`() {
        val post = post(id = 1L, commentCount = 3)
        val c = comment(id = 1L, postId = 1L, userId = 5L)
        given(commentRepository.findById(1L)).willReturn(Optional.of(c))
        given(postRepository.findById(1L)).willReturn(Optional.of(post))

        commentService.deleteComment(1L, 5L)

        verify(commentRepository).delete(c)
        assertThat(post.commentCount).isEqualTo(2)
    }

    @Test
    fun `deleteComment - 다른 사용자가 삭제 시 COMMENT_AUTHOR_MISMATCH`() {
        val c = comment(id = 1L, postId = 1L, userId = 5L)
        given(commentRepository.findById(1L)).willReturn(Optional.of(c))

        val ex = assertThrows<BusinessException> { commentService.deleteComment(1L, 99L) }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.COMMENT_AUTHOR_MISMATCH)
    }

    private fun post(id: Long = 1L, commentCount: Int = 0) =
        Post(id = id, userId = 1L, title = "t", content = "c", commentCount = commentCount)

    private fun comment(
        id: Long = 1L,
        postId: Long = 1L,
        userId: Long = 1L,
        parentCommentId: Long? = null,
        content: String = "comment",
    ) = Comment(id = id, postId = postId, userId = userId, parentCommentId = parentCommentId, content = content)
}
