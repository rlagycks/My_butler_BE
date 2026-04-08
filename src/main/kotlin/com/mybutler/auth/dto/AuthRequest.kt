package com.mybutler.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CheckUsernameRequest(
    @field:NotBlank
    @field:Size(min = 2, max = 20)
    @field:Pattern(regexp = "^[a-zA-Z0-9가-힣_]+$", message = "아이디는 영문, 숫자, 한글, 언더스코어만 사용할 수 있습니다.")
    val username: String,
)

data class RegisterRequest(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    @field:Size(min = 2, max = 20)
    @field:Pattern(regexp = "^[a-zA-Z0-9가-힣_]+$", message = "아이디는 영문, 숫자, 한글, 언더스코어만 사용할 수 있습니다.")
    val username: String,

    @field:NotBlank
    @field:Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$",
        message = "비밀번호는 8자 이상이며 영문, 숫자, 특수문자를 포함해야 합니다.",
    )
    val password: String,

    val termsAgreed: Boolean,
    val privacyAgreed: Boolean,
    val marketingAgreed: Boolean,
)

data class LoginRequest(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    val password: String,
)

data class PasswordResetRequestDto(
    @field:NotBlank
    @field:Email
    val email: String,
)

data class PasswordResetExecuteRequest(
    @field:NotBlank
    val token: String,

    @field:NotBlank
    @field:Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$",
        message = "비밀번호는 8자 이상이며 영문, 숫자, 특수문자를 포함해야 합니다.",
    )
    val newPassword: String,
)
