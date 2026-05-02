package com.mybutler.community.controller

import com.mybutler.common.response.ApiResponse
import com.mybutler.common.security.CustomUserDetails
import com.mybutler.community.dto.CreatePostRequest
import com.mybutler.community.dto.PostPageResponse
import com.mybutler.community.dto.PostResponse
import com.mybutler.community.dto.UpdatePostRequest
import com.mybutler.community.service.PostService
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
@RequestMapping("/api/v1/posts")
@SecurityRequirement(name = "Bearer Authentication")
class PostController(
    private val postService: PostService,
) {
    @Operation(summary = "게시글 작성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPost(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody request: CreatePostRequest,
    ): ApiResponse<PostResponse> {
        return ApiResponse.ok(postService.createPost(userDetails.userId, request))
    }

    @Operation(summary = "피드 조회")
    @GetMapping
    fun getFeed(
        @ParameterObject
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable,
    ): ApiResponse<PostPageResponse> {
        return ApiResponse.ok(postService.getFeed(pageable))
    }

    @Operation(summary = "게시글 상세 조회")
    @GetMapping("/{postId}")
    fun getPostDetail(
        @PathVariable postId: Long,
    ): ApiResponse<PostResponse> {
        return ApiResponse.ok(postService.getPostDetail(postId))
    }

    @Operation(summary = "게시글 수정")
    @PutMapping("/{postId}")
    fun updatePost(
        @PathVariable postId: Long,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody request: UpdatePostRequest,
    ): ApiResponse<PostResponse> {
        return ApiResponse.ok(postService.updatePost(postId, userDetails.userId, request))
    }

    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePost(
        @PathVariable postId: Long,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ) {
        postService.deletePost(postId, userDetails.userId)
    }

    @Operation(summary = "내 게시글 목록")
    @GetMapping("/me")
    fun getMyPosts(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @ParameterObject
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable,
    ): ApiResponse<PostPageResponse> {
        return ApiResponse.ok(postService.getMyPosts(userDetails.userId, pageable))
    }
}
