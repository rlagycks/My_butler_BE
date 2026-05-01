package com.mybutler.recipe.controller

import com.mybutler.common.response.ApiResponse
import com.mybutler.common.security.CustomUserDetails
import com.mybutler.recipe.dto.CreateRecipeRequest
import com.mybutler.recipe.dto.RecipeDetailResponse
import com.mybutler.recipe.dto.RecipeHomeResponse
import com.mybutler.recipe.dto.RecipePageResponse
import com.mybutler.recipe.dto.RecipeRecommendationResponse
import com.mybutler.recipe.dto.RecipeSummaryResponse
import com.mybutler.recipe.dto.UpdateRecipeRequest
import com.mybutler.recipe.entity.BaseSpirit
import com.mybutler.recipe.entity.RecipeCategory
import com.mybutler.recipe.service.RecipeService
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Recipe", description = "레시피 API")
@RestController
@RequestMapping("/api/v1/recipes")
@SecurityRequirement(name = "Bearer Authentication")
class RecipeController(
    private val recipeService: RecipeService,
) {
    @Operation(summary = "레시피 홈 조회", description = "재고 기반 가능/거의 가능 레시피 + 취향 기반 추천")
    @GetMapping("/home")
    fun getHome(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ApiResponse<RecipeHomeResponse> {
        return ApiResponse.ok(recipeService.getHome(userDetails.userId))
    }

    @Operation(summary = "기본 레시피 목록/검색")
    @GetMapping
    fun getBaseRecipes(
        @RequestParam(required = false) category: RecipeCategory?,
        @RequestParam(required = false) baseSpirit: BaseSpirit?,
        @RequestParam(required = false) difficulty: Int?,
        @RequestParam(required = false) keyword: String?,
        @ParameterObject
        @PageableDefault(size = 20, sort = ["averageRating"], direction = Sort.Direction.DESC)
        pageable: Pageable,
    ): ApiResponse<RecipePageResponse> {
        return ApiResponse.ok(recipeService.getBaseRecipes(category, baseSpirit, difficulty, keyword, pageable))
    }

    @Operation(summary = "나만의 레시피 목록/검색")
    @GetMapping("/custom")
    fun getCustomRecipes(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestParam(required = false) keyword: String?,
        @ParameterObject
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable,
    ): ApiResponse<RecipePageResponse> {
        return ApiResponse.ok(recipeService.getCustomRecipes(userDetails.userId, keyword, pageable))
    }

    @Operation(summary = "재고 기반 레시피 추천")
    @GetMapping("/recommendations/inventory")
    fun getInventoryRecommendations(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ApiResponse<RecipeRecommendationResponse> {
        return ApiResponse.ok(recipeService.getInventoryRecommendations(userDetails.userId))
    }

    @Operation(summary = "취향 기반 레시피 추천")
    @GetMapping("/recommendations/preference")
    fun getPreferenceRecommendations(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ApiResponse<List<RecipeSummaryResponse>> {
        return ApiResponse.ok(recipeService.getPreferenceRecommendations(userDetails.userId))
    }

    @Operation(summary = "레시피 상세 조회")
    @GetMapping("/{id}")
    fun getDetail(
        @PathVariable id: Long,
    ): ApiResponse<RecipeDetailResponse> {
        return ApiResponse.ok(recipeService.getDetail(id))
    }

    @Operation(summary = "나만의 레시피 등록")
    @PostMapping("/custom")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody request: CreateRecipeRequest,
    ): ApiResponse<RecipeDetailResponse> {
        return ApiResponse.ok(recipeService.create(userDetails.userId, request))
    }

    @Operation(summary = "나만의 레시피 수정")
    @PutMapping("/custom/{id}")
    fun update(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateRecipeRequest,
    ): ApiResponse<RecipeDetailResponse> {
        return ApiResponse.ok(recipeService.update(userDetails.userId, id, request))
    }

    @Operation(summary = "나만의 레시피 삭제")
    @DeleteMapping("/custom/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable id: Long,
    ) {
        recipeService.delete(userDetails.userId, id)
    }
}
