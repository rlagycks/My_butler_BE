package com.mybutler.community.repository

import com.mybutler.community.entity.PostLike
import org.springframework.data.jpa.repository.JpaRepository

interface PostLikeRepository : JpaRepository<PostLike, Long> {
    fun existsByPostIdAndUserId(postId: Long, userId: Long): Boolean
    fun findByPostIdAndUserId(postId: Long, userId: Long): PostLike?
    fun findAllByUserIdAndPostIdIn(userId: Long, postIds: List<Long>): List<PostLike>
    fun deleteAllByPostId(postId: Long)
}
