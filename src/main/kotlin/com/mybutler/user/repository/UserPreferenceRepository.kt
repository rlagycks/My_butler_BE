package com.mybutler.user.repository

import com.mybutler.user.entity.UserPreference
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserPreferenceRepository : JpaRepository<UserPreference, Long> {
    fun findByUserId(userId: Long): Optional<UserPreference>
    fun existsByUserId(userId: Long): Boolean
}
