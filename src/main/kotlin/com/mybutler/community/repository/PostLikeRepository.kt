package com.mybutler.community.repository

import com.mybutler.community.entity.PostLike
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface PostLikeRepository : JpaRepository<PostLike, Long> {
    fun existsByPostIdAndUserId(postId: Long, userId: Long): Boolean
    fun findByPostIdAndUserId(postId: Long, userId: Long): PostLike?
    fun findAllByUserIdAndPostIdIn(userId: Long, postIds: List<Long>): List<PostLike>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PostLike pl WHERE pl.postId = :postId")
    fun deleteAllByPostId(postId: Long): Int
}
