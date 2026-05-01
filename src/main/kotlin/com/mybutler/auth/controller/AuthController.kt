package com.mybutler.auth.controller

import com.mybutler.auth.dto.AuthTokens
import com.mybutler.auth.dto.CheckUsernameRequest
import com.mybutler.auth.dto.CheckUsernameResponse
import com.mybutler.auth.dto.LoginRequest
import com.mybutler.auth.dto.PasswordResetExecuteRequest
import com.mybutler.auth.dto.PasswordResetRequestDto
import com.mybutler.auth.dto.RegisterRequest
import com.mybutler.auth.dto.TokenResponse
import com.mybutler.auth.service.AuthService
import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.common.response.ApiResponse
import com.mybutler.common.security.CustomUserDetails
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
        response: HttpServletResponse,
    ): ApiResponse<TokenResponse> {
        val tokens = authService.register(request)
        setRefreshTokenCookie(response, tokens.refreshToken)
        return ApiResponse.ok(tokens.toResponse())
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        response: HttpServletResponse,
    ): ApiResponse<TokenResponse> {
        val tokens = authService.login(request)
        setRefreshTokenCookie(response, tokens.refreshToken)
        return ApiResponse.ok(tokens.toResponse())
    }

    @Operation(summary = "토큰 재발급")
    @PostMapping("/refresh")
    fun refresh(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ApiResponse<TokenResponse> {
        val refreshToken = request.cookies
            ?.find { it.name == REFRESH_TOKEN_COOKIE }
            ?.value
            ?: throw BusinessException(ErrorCode.TOKEN_INVALID)

        val tokens = authService.refresh(refreshToken)
        setRefreshTokenCookie(response, tokens.refreshToken)
        return ApiResponse.ok(tokens.toResponse())
    }

    @Operation(summary = "로그아웃", security = [SecurityRequirement(name = "Bearer Authentication")])
    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        response: HttpServletResponse,
    ): ApiResponse<Unit> {
        authService.logout(userDetails.userId)
        clearRefreshTokenCookie(response)
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

    private fun setRefreshTokenCookie(response: HttpServletResponse, token: String) {
        val cookie = Cookie(REFRESH_TOKEN_COOKIE, token).apply {
            isHttpOnly = true
            secure = true
            path = "/api/v1/auth/refresh"
            maxAge = 7 * 24 * 60 * 60
        }
        response.addCookie(cookie)
    }

    private fun clearRefreshTokenCookie(response: HttpServletResponse) {
        val cookie = Cookie(REFRESH_TOKEN_COOKIE, "").apply {
            isHttpOnly = true
            secure = true
            path = "/api/v1/auth/refresh"
            maxAge = 0
        }
        response.addCookie(cookie)
    }

    companion object {
        private const val REFRESH_TOKEN_COOKIE = "refresh_token"
    }
}
