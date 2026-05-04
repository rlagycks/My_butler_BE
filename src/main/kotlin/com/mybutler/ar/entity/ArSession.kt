package com.mybutler.ar.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "ar_sessions")
class ArSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "recipe_id", nullable = false)
    val recipeId: Long,

    @Column(name = "post_id")
    val postId: Long? = null,

    @Column(nullable = false)
    val rating: Short,

    @Column(columnDefinition = "TEXT")
    val caption: String? = null,

    @Column(name = "photo_url", length = 512, nullable = false)
    val photoUrl: String,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
