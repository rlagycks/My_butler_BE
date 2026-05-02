package com.mybutler.community.controller

import com.mybutler.common.response.ApiResponse
import com.mybutler.common.security.CustomUserDetails
import com.mybutler.community.dto.LikeResponse
import com.mybutler.community.service.PostLikeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Community", description = "커뮤니티 게시글 API")
@RestController
@RequestMapping("/api/v1/posts/{postId}/likes")
@SecurityRequirement(name = "Bearer Authentication")
class PostLikeController(
    private val postLikeService: PostLikeService,
) {
    @Operation(summary = "좋아요 토글")
    @PostMapping
    fun toggleLike(
        @PathVariable postId: Long,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ApiResponse<LikeResponse> {
        return ApiResponse.ok(postLikeService.toggleLike(postId, userDetails.userId))
    }

    @Operation(summary = "좋아요 상태 조회")
    @GetMapping
    fun getLikeStatus(
        @PathVariable postId: Long,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ApiResponse<LikeResponse> {
        return ApiResponse.ok(postLikeService.getLikeStatus(postId, userDetails.userId))
    }
}
