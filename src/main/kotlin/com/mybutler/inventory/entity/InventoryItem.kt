package com.mybutler.inventory.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Entity
@Table(name = "inventory_items")
class InventoryItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: Category,

    @Column(precision = 4, scale = 1)
    var abv: java.math.BigDecimal? = null,

    @Column(name = "capacity_ml")
    var capacityMl: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "level_status", nullable = false)
    var levelStatus: LevelStatus,

    @Column(name = "purchase_price")
    var purchasePrice: Int? = null,

    @Column(name = "is_opened", nullable = false)
    var isOpened: Boolean = false,

    @Column(name = "opened_at")
    var openedAt: LocalDateTime? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun getExpiryStatus(now: LocalDateTime = LocalDateTime.now()): ExpiryStatus {
        val openedDate = openedAt?.toLocalDate() ?: return ExpiryStatus.UNOPENED
        val elapsedDays = ChronoUnit.DAYS.between(openedDate, now.toLocalDate())

        return when {
            elapsedDays >= 14 -> ExpiryStatus.DANGER
            elapsedDays >= 7 -> ExpiryStatus.WARNING
            else -> ExpiryStatus.NORMAL
        }
    }
}

enum class Category {
    WHISKEY,
    VODKA,
    RUM,
    GIN,
    TEQUILA,
    LIQUEUR,
    OTHER,
}

enum class LevelStatus {
    FULL,
    HALF,
    LOW,
}

enum class ExpiryStatus {
    UNOPENED,
    NORMAL,
    WARNING,
    DANGER,
}
