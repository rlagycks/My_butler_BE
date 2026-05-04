package com.mybutler.auth.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class PasswordResetTokenStore(
    private val redisTemplate: StringRedisTemplate,
    @Value("\${password-reset.token-expiry-minutes:30}") private val expiryMinutes: Long,
) {
    private fun key(token: String) = "pw-reset:$token"

    fun save(token: String, email: String) {
        redisTemplate.opsForValue().set(key(token), email, Duration.ofMinutes(expiryMinutes))
    }

    fun getEmail(token: String): String? = redisTemplate.opsForValue().get(key(token))

    fun delete(token: String) {
        redisTemplate.delete(key(token))
    }
}
