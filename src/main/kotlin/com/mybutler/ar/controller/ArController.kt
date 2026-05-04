package com.mybutler.ar.controller

import com.mybutler.ar.dto.ArRecipeResponse
import com.mybutler.ar.dto.ArSessionRequest
import com.mybutler.ar.dto.ArSessionResponse
import com.mybutler.ar.service.ArService
import com.mybutler.common.response.ApiResponse
import com.mybutler.common.security.CustomUserDetails
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Tag(name = "AR", description = "AR 칵테일 제조 API")
@RestController
@RequestMapping("/api/v1/ar")
@SecurityRequirement(name = "Bearer Authentication")
class ArController(
    private val arService: ArService,
) {
    @Operation(summary = "AR 레시피 조회")
    @GetMapping("/recipes/{recipeId}")
    fun getArRecipe(
        @PathVariable recipeId: Long,
    ): ApiResponse<ArRecipeResponse> {
        return ApiResponse.ok(arService.getArRecipe(recipeId))
    }

    @Operation(summary = "AR 세션 제출 (사진 + 별점 저장)")
    @PostMapping("/sessions", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun submitSession(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestParam recipeId: Long,
        @RequestParam rating: Int?,
        @RequestParam(required = false) caption: String?,
        @RequestPart("photo") photo: MultipartFile,
    ): ApiResponse<ArSessionResponse> {
        val request = ArSessionRequest(recipeId = recipeId, rating = rating, caption = caption)
        return ApiResponse.ok(arService.submitSession(userDetails.userId, request, photo))
    }
}
