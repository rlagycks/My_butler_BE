package com.mybutler.community.repository

import com.mybutler.community.entity.PostComment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface PostCommentRepository : JpaRepository<PostComment, Long> {
    fun findByPostIdAndParentCommentIdIsNull(postId: Long, pageable: Pageable): Page<PostComment>
    fun findAllByParentCommentIdIn(parentIds: List<Long>): List<PostComment>
    fun countByParentCommentId(parentCommentId: Long): Long

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PostComment pc WHERE pc.parentCommentId = :parentCommentId")
    fun deleteAllByParentCommentId(parentCommentId: Long): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PostComment pc WHERE pc.postId = :postId")
    fun deleteAllByPostId(postId: Long): Int
}
