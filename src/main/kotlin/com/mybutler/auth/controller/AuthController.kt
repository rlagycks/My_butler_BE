package com.mybutler.auth.controller

import com.mybutler.auth.dto.CheckUsernameRequest
import com.mybutler.auth.dto.CheckUsernameResponse
import com.mybutler.auth.dto.LoginRequest
import com.mybutler.auth.dto.PasswordResetExecuteRequest
import com.mybutler.auth.dto.PasswordResetRequestDto
import com.mybutler.auth.dto.RefreshTokenRequest
import com.mybutler.auth.dto.RegisterRequest
import com.mybutler.auth.dto.TokenResponse
import com.mybutler.auth.service.AuthService
import com.mybutler.common.response.ApiResponse
import com.mybutler.common.security.CustomUserDetails
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
) {
    @Operation(summary = "아이디 중복 확인")
    @GetMapping("/check-username")
    fun checkUsername(@Valid request: CheckUsernameRequest): ApiResponse<CheckUsernameResponse> {
        return ApiResponse.ok(authService.checkUsername(request.username))
    }

    @Operation(summary = "회원가입")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @Valid @RequestBody request: RegisterRequest,
    ): ApiResponse<TokenResponse> {
        val tokens = authService.register(request)
        return ApiResponse.ok(tokens.toResponse())
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
    ): ApiResponse<TokenResponse> {
        val tokens = authService.login(request)
        return ApiResponse.ok(tokens.toResponse())
    }

    @Operation(summary = "토큰 재발급")
    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshTokenRequest,
    ): ApiResponse<TokenResponse> {
        val tokens = authService.refresh(request.refreshToken)
        return ApiResponse.ok(tokens.toResponse())
    }

    @Operation(summary = "로그아웃", security = [SecurityRequirement(name = "Bearer Authentication")])
    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ApiResponse<Unit> {
        authService.logout(userDetails.userId)
        return ApiResponse.ok()
    }

    @Operation(summary = "비밀번호 재설정 요청")
    @PostMapping("/password/reset-request")
    fun requestPasswordReset(
        @Valid @RequestBody request: PasswordResetRequestDto,
    ): ApiResponse<Unit> {
        authService.requestPasswordReset(request.email)
        return ApiResponse.ok()
    }

    @Operation(summary = "비밀번호 재설정 실행")
    @PostMapping("/password/reset")
    fun resetPassword(
        @Valid @RequestBody request: PasswordResetExecuteRequest,
    ): ApiResponse<Unit> {
        authService.resetPassword(request.token, request.newPassword)
        return ApiResponse.ok()
    }
}
