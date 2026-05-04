package com.mybutler.recipe.controller

import com.mybutler.common.response.ApiResponse
import com.mybutler.common.security.CustomUserDetails
import com.mybutler.recipe.dto.RatingPageResponse
import com.mybutler.recipe.dto.RatingResponse
import com.mybutler.recipe.dto.RatingUpsertRequest
import com.mybutler.recipe.service.RatingService
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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Rating", description = "레시피 평점 API")
@RestController
@RequestMapping("/api/v1/recipes/{recipeId}/ratings")
@SecurityRequirement(name = "Bearer Authentication")
class RatingController(
    private val ratingService: RatingService,
) {
    @Operation(summary = "평점 등록/수정 (upsert)")
    @PostMapping
    fun upsert(
        @PathVariable recipeId: Long,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody request: RatingUpsertRequest,
    ): ApiResponse<RatingResponse> {
        return ApiResponse.ok(ratingService.upsert(recipeId, userDetails.userId, request))
    }

    @Operation(summary = "평점 목록 조회")
    @GetMapping
    fun getRatings(
        @PathVariable recipeId: Long,
        @ParameterObject
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable,
    ): ApiResponse<RatingPageResponse> {
        return ApiResponse.ok(ratingService.getRatings(recipeId, pageable))
    }

    @Operation(summary = "내 평점 조회")
    @GetMapping("/me")
    fun getMyRating(
        @PathVariable recipeId: Long,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ApiResponse<RatingResponse> {
        return ApiResponse.ok(ratingService.getMyRating(recipeId, userDetails.userId))
    }

    @Operation(summary = "내 평점 삭제")
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteMyRating(
        @PathVariable recipeId: Long,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ) {
        ratingService.deleteMyRating(recipeId, userDetails.userId)
    }
}
