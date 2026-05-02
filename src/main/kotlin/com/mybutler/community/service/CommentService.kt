package com.mybutler.community.service

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.community.dto.CommentPageResponse
import com.mybutler.community.dto.CommentResponse
import com.mybutler.community.dto.CreateCommentRequest
import com.mybutler.community.dto.UpdateCommentRequest
import com.mybutler.community.entity.Comment
import com.mybutler.community.repository.CommentRepository
import com.mybutler.community.repository.PostRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CommentService(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
) {
    @Transactional
    fun createComment(postId: Long, userId: Long, request: CreateCommentRequest): CommentResponse {
        val post = postRepository.findByIdOrNull(postId)
            ?: throw BusinessException(ErrorCode.POST_NOT_FOUND)

        val comment = commentRepository.save(
            Comment(
                postId = postId,
                userId = userId,
                parentCommentId = request.parentCommentId,
                content = request.content,
            )
        )
        post.commentCount++
        return CommentResponse.from(comment)
    }

    fun getComments(postId: Long, pageable: Pageable): CommentPageResponse {
        if (!postRepository.existsById(postId)) throw BusinessException(ErrorCode.POST_NOT_FOUND)
        return CommentPageResponse.from(
            commentRepository.findByPostIdAndParentCommentIdIsNull(postId, pageable)
        )
    }

    fun getReplies(commentId: Long, pageable: Pageable): CommentPageResponse {
        if (!commentRepository.existsById(commentId)) throw BusinessException(ErrorCode.COMMENT_NOT_FOUND)
        return CommentPageResponse.from(
            commentRepository.findByParentCommentId(commentId, pageable)
        )
    }

    @Transactional
    fun updateComment(commentId: Long, userId: Long, request: UpdateCommentRequest): CommentResponse {
        val comment = findOwned(commentId, userId)
        comment.content = request.content
        return CommentResponse.from(comment)
    }

    @Transactional
    fun deleteComment(commentId: Long, userId: Long) {
        val comment = findOwned(commentId, userId)
        val post = postRepository.findByIdOrNull(comment.postId)
        commentRepository.delete(comment)
        post?.let { it.commentCount = maxOf(0, it.commentCount - 1) }
    }

    private fun findOwned(commentId: Long, userId: Long): Comment {
        val comment = commentRepository.findByIdOrNull(commentId)
            ?: throw BusinessException(ErrorCode.COMMENT_NOT_FOUND)
        if (comment.userId != userId) throw BusinessException(ErrorCode.COMMENT_AUTHOR_MISMATCH)
        return comment
    }
}
