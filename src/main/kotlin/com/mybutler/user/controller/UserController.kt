package com.mybutler.user.controller

import com.mybutler.common.response.ApiResponse
import com.mybutler.common.security.CustomUserDetails
import com.mybutler.user.dto.*
import com.mybutler.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "Bearer Authentication")
class UserController(
    private val userService: UserService,
) {
    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    fun getMyProfile(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ApiResponse<UserProfileResponse> {
        return ApiResponse.ok(userService.getMyProfile(userDetails.userId))
    }

    @Operation(summary = "기본 정보 저장 (온보딩 Step 2)")
    @PatchMapping("/me/profile")
    fun updateProfile(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody request: UpdateProfileRequest,
    ): ApiResponse<UserProfileResponse> {
        return ApiResponse.ok(userService.updateProfile(userDetails.userId, request))
    }

    @Operation(summary = "취향 저장 (온보딩 Step 3)")
    @PostMapping("/me/preferences")
    fun savePreferences(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody request: SavePreferencesRequest,
    ): ApiResponse<UserPreferenceResponse> {
        return ApiResponse.ok(userService.savePreferences(userDetails.userId, request))
    }

    @Operation(summary = "취향 조회")
    @GetMapping("/me/preferences")
    fun getPreferences(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ApiResponse<UserPreferenceResponse> {
        return ApiResponse.ok(userService.getPreferences(userDetails.userId))
    }

    @Operation(summary = "닉네임 변경")
    @PatchMapping("/me/username")
    fun updateUsername(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody request: UpdateUsernameRequest,
    ): ApiResponse<UsernameResponse> {
        return ApiResponse.ok(userService.updateUsername(userDetails.userId, request))
    }
}
