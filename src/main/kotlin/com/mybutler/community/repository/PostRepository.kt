package com.mybutler.community.repository

import com.mybutler.community.entity.Post
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface PostRepository : JpaRepository<Post, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Post p WHERE p.id = :id")
    fun findByIdForUpdate(id: Long): Post?

    fun findByUserId(userId: Long, pageable: Pageable): Page<Post>
}
