package com.mybutler.community.repository

import com.mybutler.community.entity.PostComment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface PostCommentRepository : JpaRepository<PostComment, Long> {
    fun findByPostIdAndParentCommentIdIsNull(postId: Long, pageable: Pageable): Page<PostComment>
    fun findAllByParentCommentIdIn(parentIds: List<Long>): List<PostComment>
    fun countByParentCommentId(parentCommentId: Long): Long
    fun deleteAllByParentCommentId(parentCommentId: Long)
    fun deleteAllByPostId(postId: Long)
}
