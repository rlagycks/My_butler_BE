package com.mybutler.community.dto

import com.mybutler.community.entity.Post
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.data.domain.Page
import java.time.LocalDateTime

data class CreatePostRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val title: String,

    @field:NotBlank
    val content: String,

    val imageUrls: List<String> = emptyList(),
)

data class UpdatePostRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val title: String,

    @field:NotBlank
    val content: String,

    val imageUrls: List<String> = emptyList(),
)

data class PostResponse(
    val id: Long,
    val userId: Long,
    val title: String,
    val content: String,
    val thumbnailUrl: String?,
    val imageUrls: List<String>,
    val likeCount: Int,
    val commentCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(post: Post) = PostResponse(
            id = post.id,
            userId = post.userId,
            title = post.title,
            content = post.content,
            thumbnailUrl = post.images.firstOrNull()?.imageUrl,
            imageUrls = post.images.map { it.imageUrl },
            likeCount = post.likeCount,
            commentCount = post.commentCount,
            createdAt = post.createdAt,
            updatedAt = post.updatedAt,
        )
    }
}

data class PostPageResponse(
    val content: List<PostResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
) {
    companion object {
        fun from(page: Page<Post>) = PostPageResponse(
            content = page.content.map(PostResponse::from),
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            last = page.isLast,
        )
    }
}
