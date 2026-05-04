package com.mybutler.community.controller

import com.mybutler.common.response.ApiResponse
import com.mybutler.common.security.CustomUserDetails
import com.mybutler.community.dto.CommentCreateResponse
import com.mybutler.community.dto.CommentPageResponse
import com.mybutler.community.dto.CreateCommentRequest
import com.mybutler.community.dto.CreatePostRequest
import com.mybutler.community.dto.FeedPageResponse
import com.mybutler.community.dto.LikeResponse
import com.mybutler.community.dto.PostCreateResponse
import com.mybutler.community.dto.PostDetailResponse
import com.mybutler.community.entity.PostType
import com.mybutler.community.service.PostCommentService
import com.mybutler.community.service.PostLikeService
import com.mybutler.community.service.PostService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Tag(name = "Community", description = "커뮤니티 API")
@RestController
@RequestMapping("/api/v1/posts")
@SecurityRequirement(name = "Bearer Authentication")
class PostController(
    private val postService: PostService,
    private val postLikeService: PostLikeService,
    private val postCommentService: PostCommentService,
) {
    @Operation(summary = "피드 목록 조회")
    @GetMapping
    fun getPostFeed(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestParam(defaultValue = "LATEST") sort: String,
        @ParameterObject @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<FeedPageResponse> {
        return ApiResponse.ok(postService.getPostFeed(userDetails.userId, sort, pageable))
    }

    @Operation(summary = "마이프로필 게시물 목록 (추가 페이징)")
    @GetMapping("/my")
    fun getMyPosts(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @ParameterObject @PageableDefault(size = 9) pageable: Pageable,
    ): ApiResponse<FeedPageResponse> {
        return ApiResponse.ok(postService.getMyPosts(userDetails.userId, pageable))
    }

    @Operation(summary = "게시물 작성")
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun createPost(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestPart request: CreatePostRequest,
        @RequestPart(required = false) images: List<MultipartFile>?,
    ): ApiResponse<PostCreateResponse> {
        return ApiResponse.ok(
            postService.createPost(userDetails.userId, request, images ?: emptyList()),
        )
    }

    @Operation(summary = "게시물 상세 조회")
    @GetMapping("/{id}")
    fun getPostDetail(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable id: Long,
    ): ApiResponse<PostDetailResponse> {
        return ApiResponse.ok(postService.getPostDetail(id, userDetails.userId))
    }

    @Operation(summary = "게시물 삭제")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePost(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable id: Long,
    ) {
        postService.deletePost(id, userDetails.userId)
    }

    @Operation(summary = "좋아요 추가")
    @PostMapping("/{id}/likes")
    fun addLike(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable id: Long,
    ): ApiResponse<LikeResponse> {
        return ApiResponse.ok(postLikeService.addLike(id, userDetails.userId))
    }

    @Operation(summary = "좋아요 취소")
    @DeleteMapping("/{id}/likes")
    fun removeLike(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable id: Long,
    ): ApiResponse<LikeResponse> {
        return ApiResponse.ok(postLikeService.removeLike(id, userDetails.userId))
    }

    @Operation(summary = "댓글 목록 조회")
    @GetMapping("/{id}/comments")
    fun getComments(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable id: Long,
        @ParameterObject @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.ASC) pageable: Pageable,
    ): ApiResponse<CommentPageResponse> {
        return ApiResponse.ok(postCommentService.getComments(id, userDetails.userId, pageable))
    }

    @Operation(summary = "댓글 작성")
    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    fun addComment(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable id: Long,
        @RequestBody request: CreateCommentRequest,
    ): ApiResponse<CommentCreateResponse> {
        return ApiResponse.ok(postCommentService.addComment(id, userDetails.userId, request.content))
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/{id}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteComment(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable id: Long,
        @PathVariable commentId: Long,
    ) {
        postCommentService.deleteComment(id, commentId, userDetails.userId)
    }

    @Operation(summary = "대댓글 작성")
    @PostMapping("/{id}/comments/{commentId}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    fun addReply(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable id: Long,
        @PathVariable commentId: Long,
        @RequestBody request: CreateCommentRequest,
    ): ApiResponse<CommentCreateResponse> {
        return ApiResponse.ok(postCommentService.addReply(id, commentId, userDetails.userId, request.content))
    }

    @Operation(summary = "대댓글 삭제")
    @DeleteMapping("/{id}/comments/{commentId}/replies/{replyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteReply(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable id: Long,
        @PathVariable commentId: Long,
        @PathVariable replyId: Long,
    ) {
        postCommentService.deleteReply(id, commentId, replyId, userDetails.userId)
    }
}
