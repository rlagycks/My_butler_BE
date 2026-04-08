package com.mybutler.auth.repository

import com.mybutler.auth.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): Optional<RefreshToken>
    fun deleteByUserId(userId: Long)
    fun deleteByToken(token: String)
}
