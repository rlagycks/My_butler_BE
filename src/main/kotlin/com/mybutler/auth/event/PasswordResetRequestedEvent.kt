package com.mybutler.auth.event

data class PasswordResetRequestedEvent(
    val email: String,
    val token: String,
)
