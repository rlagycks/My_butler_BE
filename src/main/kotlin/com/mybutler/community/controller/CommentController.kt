package com.mybutler.community.controller

import com.mybutler.common.response.ApiResponse
import com.mybutler.common.security.CustomUserDetails
import com.mybutler.community.dto.CommentPageResponse
import com.mybutler.community.dto.CommentResponse
import com.mybutler.community.dto.CreateCommentRequest
import com.mybutler.community.dto.UpdateCommentRequest
import com.mybutler.community.service.CommentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Community", description = "커뮤니티 게시글 API")
@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
@SecurityRequirement(name = "Bearer Authentication")
class CommentController(
    private val commentService: CommentService,
) {
    @Operation(summary = "댓글 작성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createComment(
        @PathVariable postId: Long,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody request: CreateCommentRequest,
    ): ApiResponse<CommentResponse> {
        return ApiResponse.ok(commentService.createComment(postId, userDetails.userId, request))
    }

    @Operation(summary = "댓글 목록 조회 (최상위)")
    @GetMapping
    fun getComments(
        @PathVariable postId: Long,
        @ParameterObject
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.ASC)
        pageable: Pageable,
    ): ApiResponse<CommentPageResponse> {
        return ApiResponse.ok(commentService.getComments(postId, pageable))
    }

    @Operation(summary = "댓글 수정")
    @PutMapping("/{commentId}")
    fun updateComment(
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody request: UpdateCommentRequest,
    ): ApiResponse<CommentResponse> {
        return ApiResponse.ok(commentService.updateComment(commentId, userDetails.userId, request))
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteComment(
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ) {
        commentService.deleteComment(commentId, userDetails.userId)
    }

    @Operation(summary = "대댓글 목록 조회")
    @GetMapping("/{commentId}/replies")
    fun getReplies(
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
        @ParameterObject
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.ASC)
        pageable: Pageable,
    ): ApiResponse<CommentPageResponse> {
        return ApiResponse.ok(commentService.getReplies(commentId, pageable))
    }
}
