package com.mybutler.user.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "user_preferences")
class UserPreference(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val userId: Long,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_taste_preferences", joinColumns = [JoinColumn(name = "user_preference_id")])
    @Column(name = "taste")
    @Enumerated(EnumType.STRING)
    var tastePreferences: MutableSet<TastePreference> = mutableSetOf(),

    @Enumerated(EnumType.STRING)
    @Column
    var preferredAbv: PreferredAbv? = null,

    @Enumerated(EnumType.STRING)
    @Column
    var experienceLevel: ExperienceLevel? = null,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)

enum class TastePreference { SWEET, SOUR, BITTER, STRONG, LIGHT, SMOKY, SPICY, FRUITY, CITRUS, DRY }

enum class PreferredAbv { LOW, MEDIUM, HIGH }

enum class ExperienceLevel { BEGINNER, INTERMEDIATE, ADVANCED }
