package com.mybutler.community.repository

import com.mybutler.community.entity.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface PostRepository : JpaRepository<Post, Long> {
    fun findByUserId(userId: Long, pageable: Pageable): Page<Post>
}
