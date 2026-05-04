package com.mybutler.community.dto

import com.mybutler.community.entity.PostType
import java.time.LocalDateTime

data class AuthorDto(
    val id: Long,
    val username: String,
)

data class RecipeTagDto(
    val recipeId: Long,
    val recipeName: String,
)

data class PostSummaryResponse(
    val id: Long,
    val type: PostType,
    val author: AuthorDto,
    val imageUrls: List<String>,
    val caption: String?,
    val recipeTag: RecipeTagDto?,
    val rating: Short?,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val isArGenerated: Boolean,
    val createdAt: LocalDateTime,
)

data class FeedPageResponse(
    val content: List<PostSummaryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
)

data class PostCreateResponse(
    val id: Long,
    val type: PostType,
    val imageUrls: List<String>,
    val caption: String?,
    val createdAt: LocalDateTime,
)

data class ReplyResponse(
    val id: Long,
    val author: AuthorDto,
    val content: String,
    val isMyComment: Boolean,
    val createdAt: LocalDateTime,
)

data class CommentResponse(
    val id: Long,
    val author: AuthorDto,
    val content: String,
    val replyCount: Int,
    val isMyComment: Boolean,
    val createdAt: LocalDateTime,
    val replies: List<ReplyResponse>,
)

data class CommentPageResponse(
    val content: List<CommentResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
)

data class PostDetailPostResponse(
    val id: Long,
    val type: PostType,
    val author: AuthorDto,
    val imageUrls: List<String>,
    val caption: String?,
    val recipeTag: RecipeTagDto?,
    val rating: Short?,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val isArGenerated: Boolean,
    val isMyPost: Boolean,
    val createdAt: LocalDateTime,
)

data class PostDetailResponse(
    val post: PostDetailPostResponse,
    val comments: CommentPageResponse,
)

data class LikeResponse(
    val postId: Long,
    val likeCount: Int,
    val isLiked: Boolean,
)

data class CommentCreateResponse(
    val id: Long,
    val content: String,
    val createdAt: LocalDateTime,
)

data class MyPostSummaryResponse(
    val id: Long,
    val type: PostType,
    val imageUrls: List<String>,
    val likeCount: Int,
    val commentCount: Int,
    val createdAt: LocalDateTime,
)

data class MyPostPageResponse(
    val content: List<MyPostSummaryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
)

data class MyProfileUserResponse(
    val id: Long,
    val username: String,
    val email: String,
)

data class MyProfileStatsResponse(
    val postCount: Long,
    val receivedLikeCount: Long,
    val myRecipeCount: Long,
)

data class MyProfileResponse(
    val profile: MyProfileUserResponse,
    val stats: MyProfileStatsResponse,
    val posts: MyPostPageResponse,
)
