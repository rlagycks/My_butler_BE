package com.mybutler.auth.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

@Component
class PasswordResetEmailComposer(
    @Value("\${password-reset.mail-subject:[My Butler] 비밀번호 재설정 안내}") private val mailSubject: String,
    @Value("\${password-reset.reset-url}") private val passwordResetUrl: String,
    @Value("\${password-reset.token-expiry-minutes:30}") private val expiryMinutes: Long,
    @Value("classpath:mail/password-reset.txt") private val templateResource: Resource,
) {
    private val template by lazy {
        templateResource.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }

    fun subject(): String = mailSubject

    fun body(token: String): String {
        return template
            .replace("{{resetUrl}}", "$passwordResetUrl?token=$token")
            .replace("{{expiryMinutes}}", expiryMinutes.toString())
    }
}
