package com.mybutler.common

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.common.security.JwtTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class JwtTokenProviderTest {

    private val secret = "test-secret-must-be-at-least-32-characters-long"
    private val accessTokenExpiryMs = 3_600_000L

    private val jwtTokenProvider = JwtTokenProvider(secret, accessTokenExpiryMs)

    @Test
    fun `createAccessToken - userId를 subject로 포함한 토큰 생성`() {
        val token = jwtTokenProvider.createAccessToken(1L, "test@email.com", "testuser")

        assertThat(token).isNotBlank()
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(1L)
    }

    @Test
    fun `validateToken - 유효한 토큰 검증 성공`() {
        val token = jwtTokenProvider.createAccessToken(1L, "test@email.com", "testuser")

        // 예외 없이 통과해야 함
        jwtTokenProvider.validateToken(token)
    }

    @Test
    fun `validateToken - 만료된 토큰 TOKEN_EXPIRED 예외`() {
        val expiredProvider = JwtTokenProvider(secret, -1L)
        val token = expiredProvider.createAccessToken(1L, "test@email.com", "testuser")

        assertThatThrownBy { jwtTokenProvider.validateToken(token) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.TOKEN_EXPIRED)
    }

    @Test
    fun `validateToken - 잘못된 토큰 TOKEN_INVALID 예외`() {
        assertThatThrownBy { jwtTokenProvider.validateToken("invalid.token.here") }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.TOKEN_INVALID)
    }

    @Test
    fun `createRefreshToken - UUID 형식 반환`() {
        val refreshToken = jwtTokenProvider.createRefreshToken()
        assertThat(refreshToken).isNotBlank()
        assertThat(refreshToken).matches("[0-9a-f-]{36}")
    }
}
