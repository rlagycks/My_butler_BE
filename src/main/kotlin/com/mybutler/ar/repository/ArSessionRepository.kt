package com.mybutler.ar.repository

import com.mybutler.ar.entity.ArSession
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ArSessionRepository : JpaRepository<ArSession, Long> {
    fun findAllByUserId(userId: Long, pageable: Pageable): Page<ArSession>
}
