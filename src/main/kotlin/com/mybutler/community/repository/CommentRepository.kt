package com.mybutler.community.repository

import com.mybutler.community.entity.Comment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CommentRepository : JpaRepository<Comment, Long> {
    fun countByPostId(postId: Long): Long
    fun findByPostIdAndParentCommentIdIsNull(postId: Long, pageable: Pageable): Page<Comment>
    fun findByParentCommentId(parentCommentId: Long, pageable: Pageable): Page<Comment>
}
