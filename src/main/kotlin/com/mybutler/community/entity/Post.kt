package com.mybutler.community.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "posts")
class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "author_id", nullable = false)
    val authorId: Long,

    @Column(name = "recipe_id")
    var recipeId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val type: PostType,

    @Column(columnDefinition = "TEXT")
    var caption: String? = null,

    @Column(name = "is_ar_generated", nullable = false)
    val isArGenerated: Boolean = false,

    @Column(name = "ar_rating")
    var arRating: Short? = null,

    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0,

    @Column(name = "comment_count", nullable = false)
    var commentCount: Int = 0,

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @BatchSize(size = BATCH_SIZE)
    val images: MutableList<PostImage> = mutableListOf(),

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        const val BATCH_SIZE = 100
    }
}

enum class PostType {
    PHOTO, TEXT
}
