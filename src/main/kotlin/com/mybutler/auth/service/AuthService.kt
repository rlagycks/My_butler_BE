package com.mybutler.auth.service

import com.mybutler.auth.dto.AuthTokens
import com.mybutler.auth.dto.CheckUsernameResponse
import com.mybutler.auth.dto.LoginRequest
import com.mybutler.auth.dto.PasswordResetRequestDto
import com.mybutler.auth.dto.RegisterRequest
import com.mybutler.auth.entity.RefreshToken
import com.mybutler.auth.entity.User
import com.mybutler.auth.repository.RefreshTokenRepository
import com.mybutler.auth.repository.UserRepository
import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.common.security.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    @Value("\${jwt.refresh-token-expiry-ms}") private val refreshTokenExpiryMs: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun checkUsername(username: String): CheckUsernameResponse {
        val available = !userRepository.existsByUsername(username)
        return CheckUsernameResponse(available = available)
    }

    @Transactional
    fun register(request: RegisterRequest): AuthTokens {
        if (!request.termsAgreed || !request.privacyAgreed) {
            throw BusinessException(ErrorCode.TERMS_NOT_AGREED)
        }
        if (userRepository.existsByEmail(request.email)) {
            throw BusinessException(ErrorCode.DUPLICATE_EMAIL)
        }
        if (userRepository.existsByUsername(request.username)) {
            throw BusinessException(ErrorCode.DUPLICATE_USERNAME)
        }

        val user = userRepository.save(
            User(
                email = request.email,
                username = request.username,
                password = passwordEncoder.encode(request.password),
                termsAgreed = request.termsAgreed,
                privacyAgreed = request.privacyAgreed,
                marketingAgreed = request.marketingAgreed,
            )
        )

        return issueTokens(user)
    }

    @Transactional
    fun login(request: LoginRequest): AuthTokens {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { BusinessException(ErrorCode.INVALID_CREDENTIALS) }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw BusinessException(ErrorCode.INVALID_CREDENTIALS)
        }

        return issueTokens(user)
    }

    @Transactional
    fun refresh(refreshToken: String): AuthTokens {
        val stored = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow { BusinessException(ErrorCode.TOKEN_INVALID) }

        if (stored.expiresAt.isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored)
            throw BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED)
        }

        val user = userRepository.findByIdOrNull(stored.userId)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)

        refreshTokenRepository.delete(stored)
        return issueTokens(user)
    }

    @Transactional
    fun logout(userId: Long) {
        refreshTokenRepository.deleteByUserId(userId)
    }

    @Transactional
    fun requestPasswordReset(email: String) {
        val user = userRepository.findByEmail(email).orElse(null) ?: return
        // MVP: 로그에 토큰 출력 (실제 서비스에서는 이메일 발송)
        val resetToken = UUID.randomUUID().toString()
        log.info("Password reset token for {}: {}", user.email, resetToken)
    }

    @Transactional
    fun resetPassword(token: String, newPassword: String) {
        // MVP: Redis 연동 전까지 stub
        throw BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED)
    }

    private fun issueTokens(user: User): AuthTokens {
        val accessToken = jwtTokenProvider.createAccessToken(user.id, user.email, user.username)
        val refreshToken = jwtTokenProvider.createRefreshToken()
        val expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpiryMs / 1000)

        refreshTokenRepository.save(
            RefreshToken(
                userId = user.id,
                token = refreshToken,
                expiresAt = expiresAt,
            )
        )

        return AuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            onboardingCompleted = user.onboardingCompleted,
        )
    }
}
