package com.mybutler.auth.dto

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val onboardingCompleted: Boolean,
)

data class CheckUsernameResponse(
    val available: Boolean,
)

// Internal use only — not serialized to client
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val onboardingCompleted: Boolean,
) {
    fun toResponse() = TokenResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
        onboardingCompleted = onboardingCompleted,
    )
}
