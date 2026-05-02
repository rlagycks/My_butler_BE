package com.mybutler.recipe.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "recipes")
class Recipe(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "thumbnail_url", length = 512)
    var thumbnailUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var category: RecipeCategory,

    @Enumerated(EnumType.STRING)
    @Column(name = "base_spirit", length = 32)
    var baseSpirit: BaseSpirit? = null,

    @Column(nullable = false)
    var difficulty: Int = 1,

    @Column(name = "estimated_minutes")
    var estimatedMinutes: Int? = null,

    @Column(precision = 4, scale = 1)
    var abv: BigDecimal? = null,

    @Column(name = "average_rating", precision = 3, scale = 2, nullable = false)
    var averageRating: BigDecimal = BigDecimal.ZERO,

    @Column(name = "rating_count", nullable = false)
    var ratingCount: Int = 0,

    @Column(name = "is_custom", nullable = false)
    val isCustom: Boolean = false,

    @Column(name = "author_id")
    val authorId: Long? = null,

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "recipe_taste_tags", joinColumns = [JoinColumn(name = "recipe_id")])
    @Column(name = "taste_tag", length = 32)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = BATCH_SIZE)
    var tasteTags: MutableSet<TasteTag> = mutableSetOf(),

    @OneToMany(mappedBy = "recipe", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @BatchSize(size = BATCH_SIZE)
    val ingredients: MutableList<RecipeIngredient> = mutableListOf(),

    @OneToMany(mappedBy = "recipe", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC")
    @BatchSize(size = BATCH_SIZE)
    val steps: MutableList<RecipeStep> = mutableListOf(),

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

enum class RecipeCategory {
    CLASSIC,
    TROPICAL,
    WHISKEY,
    GIN,
    VODKA,
    NON_ALCOHOLIC,
}

enum class BaseSpirit {
    WHISKEY,
    VODKA,
    RUM,
    GIN,
    TEQUILA,
    LIQUEUR,
    OTHER,
    NONE,
}

enum class TasteTag {
    SWEET,
    SOUR,
    BITTER,
    STRONG,
    LIGHT,
}
