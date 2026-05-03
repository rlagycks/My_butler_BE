package com.mybutler.community.repository

import com.mybutler.community.entity.Post
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface PostRepository : JpaRepository<Post, Long> {
    @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
    fun findAllForFeedLatest(pageable: Pageable): Page<Post>

    @Query("SELECT p FROM Post p ORDER BY p.likeCount DESC, p.commentCount DESC, p.createdAt DESC")
    fun findAllForFeedPopular(pageable: Pageable): Page<Post>

    fun findAllByAuthorId(authorId: Long, pageable: Pageable): Page<Post>

    fun countByAuthorId(authorId: Long): Long

    @Query("SELECT COALESCE(SUM(p.likeCount), 0) FROM Post p WHERE p.authorId = :authorId")
    fun sumLikeCountByAuthorId(authorId: Long): Long

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Post p WHERE p.id = :id")
    fun findByIdForUpdate(id: Long): Post?
}
