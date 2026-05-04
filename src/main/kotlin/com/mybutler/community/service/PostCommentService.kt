package com.mybutler.community.service

import com.mybutler.auth.repository.UserRepository
import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.community.dto.AuthorDto
import com.mybutler.community.dto.CommentCreateResponse
import com.mybutler.community.dto.CommentPageResponse
import com.mybutler.community.dto.CommentResponse
import com.mybutler.community.dto.ReplyResponse
import com.mybutler.community.entity.PostComment
import com.mybutler.community.event.CommentRepliedEvent
import com.mybutler.community.event.PostCommentedEvent
import com.mybutler.community.repository.PostCommentRepository
import com.mybutler.community.repository.PostRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostCommentService(
    private val postRepository: PostRepository,
    private val postCommentRepository: PostCommentRepository,
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    fun getComments(postId: Long, currentUserId: Long, pageable: Pageable): CommentPageResponse {
        val page = postCommentRepository.findByPostIdAndParentCommentIdIsNull(postId, pageable)
        val topLevelComments = page.content

        val replies = if (topLevelComments.isNotEmpty()) {
            postCommentRepository.findAllByParentCommentIdIn(topLevelComments.map { it.id })
        } else {
            emptyList()
        }

        val allAuthorIds = (topLevelComments.map { it.authorId } + replies.map { it.authorId }).toSet()
        val userMap = userRepository.findAllById(allAuthorIds).associateBy { it.id }
        val repliesByParent = replies.groupBy { it.parentCommentId!! }

        val content = topLevelComments.map { comment ->
            val author = userMap[comment.authorId]
            val commentReplies = repliesByParent[comment.id] ?: emptyList()
            CommentResponse(
                id = comment.id,
                author = AuthorDto(id = author?.id ?: comment.authorId, username = author?.username ?: ""),
                content = comment.content,
                replyCount = commentReplies.size,
                isMyComment = comment.authorId == currentUserId,
                createdAt = comment.createdAt,
                replies = commentReplies.map { reply ->
                    val replyAuthor = userMap[reply.authorId]
                    ReplyResponse(
                        id = reply.id,
                        author = AuthorDto(id = replyAuthor?.id ?: reply.authorId, username = replyAuthor?.username ?: ""),
                        content = reply.content,
                        isMyComment = reply.authorId == currentUserId,
                        createdAt = reply.createdAt,
                    )
                },
            )
        }

        return CommentPageResponse(
            content = content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            last = page.isLast,
        )
    }

    @Transactional
    fun addComment(postId: Long, userId: Long, content: String): CommentCreateResponse {
        if (content.isBlank()) throw BusinessException(ErrorCode.COMMENT_CONTENT_REQUIRED)
        val post = postRepository.findByIdForUpdate(postId)
            ?: throw BusinessException(ErrorCode.POST_NOT_FOUND)

        val comment = postCommentRepository.save(
            PostComment(postId = postId, authorId = userId, content = content),
        )
        post.commentCount++
        eventPublisher.publishEvent(PostCommentedEvent(postId = postId, postAuthorId = post.authorId, actorUserId = userId))

        return CommentCreateResponse(id = comment.id, content = comment.content, createdAt = comment.createdAt)
    }

    @Transactional
    fun deleteComment(postId: Long, commentId: Long, userId: Long) {
        val comment = postCommentRepository.findByIdOrNull(commentId)
            ?: throw BusinessException(ErrorCode.COMMENT_NOT_FOUND)
        if (comment.authorId != userId) throw BusinessException(ErrorCode.COMMENT_AUTHOR_MISMATCH)
        if (comment.postId != postId || comment.isReply()) throw BusinessException(ErrorCode.COMMENT_INVALID_TARGET)

        val post = postRepository.findByIdForUpdate(postId)
            ?: throw BusinessException(ErrorCode.POST_NOT_FOUND)

        val replyCount = postCommentRepository.countByParentCommentId(commentId)
        postCommentRepository.deleteAllByParentCommentId(commentId)
        postCommentRepository.delete(comment)
        post.commentCount = maxOf(0, post.commentCount - 1 - replyCount.toInt())
    }

    @Transactional
    fun addReply(postId: Long, parentCommentId: Long, userId: Long, content: String): CommentCreateResponse {
        if (content.isBlank()) throw BusinessException(ErrorCode.COMMENT_CONTENT_REQUIRED)
        val parentComment = postCommentRepository.findByIdOrNull(parentCommentId)
            ?: throw BusinessException(ErrorCode.COMMENT_NOT_FOUND)
        if (parentComment.postId != postId) throw BusinessException(ErrorCode.COMMENT_INVALID_TARGET)
        if (parentComment.isReply()) throw BusinessException(ErrorCode.COMMENT_REPLY_DEPTH_EXCEEDED)

        val post = postRepository.findByIdForUpdate(postId)
            ?: throw BusinessException(ErrorCode.POST_NOT_FOUND)

        val reply = postCommentRepository.save(
            PostComment(postId = postId, parentCommentId = parentCommentId, authorId = userId, content = content),
        )
        post.commentCount++
        eventPublisher.publishEvent(
            CommentRepliedEvent(
                postId = postId,
                parentCommentAuthorId = parentComment.authorId,
                actorUserId = userId,
            ),
        )

        return CommentCreateResponse(id = reply.id, content = reply.content, createdAt = reply.createdAt)
    }

    @Transactional
    fun deleteReply(postId: Long, commentId: Long, replyId: Long, userId: Long) {
        val reply = postCommentRepository.findByIdOrNull(replyId)
            ?: throw BusinessException(ErrorCode.COMMENT_NOT_FOUND)
        if (reply.authorId != userId) throw BusinessException(ErrorCode.COMMENT_AUTHOR_MISMATCH)
        if (reply.postId != postId || reply.parentCommentId != commentId) {
            throw BusinessException(ErrorCode.COMMENT_INVALID_TARGET)
        }

        val post = postRepository.findByIdForUpdate(postId)
            ?: throw BusinessException(ErrorCode.POST_NOT_FOUND)

        postCommentRepository.delete(reply)
        post.commentCount = maxOf(0, post.commentCount - 1)
    }
}
