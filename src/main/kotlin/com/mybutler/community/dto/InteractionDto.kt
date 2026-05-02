package com.mybutler.community.dto

import com.mybutler.community.entity.Comment
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Page
import java.time.LocalDateTime

data class CreateCommentRequest(
    @field:NotBlank val content: String,
    val parentCommentId: Long? = null,
)

data class UpdateCommentRequest(
    @field:NotBlank val content: String,
)

data class CommentResponse(
    val id: Long,
    val postId: Long,
    val userId: Long,
    val parentCommentId: Long?,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(comment: Comment) = CommentResponse(
            id = comment.id,
            postId = comment.postId,
            userId = comment.userId,
            parentCommentId = comment.parentCommentId,
            content = comment.content,
            createdAt = comment.createdAt,
            updatedAt = comment.updatedAt,
        )
    }
}

data class CommentPageResponse(
    val content: List<CommentResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
) {
    companion object {
        fun from(page: Page<Comment>) = CommentPageResponse(
            content = page.content.map(CommentResponse::from),
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            last = page.isLast,
        )
    }
}

data class LikeResponse(
    val liked: Boolean,
    val likeCount: Int,
)
