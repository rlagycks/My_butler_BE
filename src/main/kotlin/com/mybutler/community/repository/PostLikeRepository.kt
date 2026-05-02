package com.mybutler.community.repository

import com.mybutler.community.entity.PostLike
import org.springframework.data.jpa.repository.JpaRepository

interface PostLikeRepository : JpaRepository<PostLike, Long> {
    fun countByPostId(postId: Long): Long
    fun existsByPostIdAndUserId(postId: Long, userId: Long): Boolean
    fun deleteByPostIdAndUserId(postId: Long, userId: Long)
}
