package com.mybutler.common.security

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.access-token-expiry-ms}") private val accessTokenExpiryMs: Long,
) {
    private val signingKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun createAccessToken(userId: Long, email: String, username: String): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("username", username)
            .issuedAt(now)
            .expiration(Date(now.time + accessTokenExpiryMs))
            .signWith(signingKey)
            .compact()
    }

    fun createRefreshToken(): String = UUID.randomUUID().toString()

    fun getUserId(token: String): Long {
        return parseClaims(token).subject.toLong()
    }

    fun validateToken(token: String) {
        parseClaims(token)
    }

    private fun parseClaims(token: String) = try {
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
    } catch (e: ExpiredJwtException) {
        throw BusinessException(ErrorCode.TOKEN_EXPIRED)
    } catch (e: JwtException) {
        throw BusinessException(ErrorCode.TOKEN_INVALID)
    }
}
