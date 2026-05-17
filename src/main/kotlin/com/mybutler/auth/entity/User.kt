package com.mybutler.auth.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false, unique = true)
    var username: String,

    @Column(nullable = false)
    var password: String,

    @Enumerated(EnumType.STRING)
    @Column
    var gender: Gender? = null,

    @Enumerated(EnumType.STRING)
    @Column
    var ageGroup: AgeGroup? = null,

    @Enumerated(EnumType.STRING)
    @Column
    var drinkingFrequency: DrinkingFrequency? = null,

    @Column(name = "birth_date")
    var birthDate: LocalDate? = null,

    @Column(name = "profile_image_url", length = 512)
    var profileImageUrl: String? = null,

    @Column(nullable = false)
    var onboardingCompleted: Boolean = false,

    @Column(nullable = false)
    val termsAgreed: Boolean = false,

    @Column(nullable = false)
    val privacyAgreed: Boolean = false,

    @Column(nullable = false)
    val marketingAgreed: Boolean = false,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)

enum class Gender { MALE, FEMALE, OTHER }

enum class AgeGroup { TWENTIES, THIRTIES, FORTIES, FIFTIES_PLUS }

enum class DrinkingFrequency { RARELY, MONTHLY, WEEKLY, DAILY }
