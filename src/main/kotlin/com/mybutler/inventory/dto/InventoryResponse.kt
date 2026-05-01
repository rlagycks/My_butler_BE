package com.mybutler.inventory.dto

import com.mybutler.inventory.entity.Category
import com.mybutler.inventory.entity.ExpiryStatus
import com.mybutler.inventory.entity.InventoryItem
import com.mybutler.inventory.entity.LevelStatus
import org.springframework.data.domain.Page
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class InventoryItemSummaryResponse(
    val id: Long,
    val name: String,
    val category: Category,
    val abv: BigDecimal?,
    val capacityMl: Int?,
    val levelStatus: LevelStatus,
    val purchasePrice: Int?,
    val isOpened: Boolean,
    val openedAt: LocalDateTime?,
    val expiryStatus: ExpiryStatus,
    val dDay: Long?,
) {
    companion object {
        fun from(item: InventoryItem): InventoryItemSummaryResponse {
            val now = LocalDateTime.now()
            return InventoryItemSummaryResponse(
                id = item.id,
                name = item.name,
                category = item.category,
                abv = item.abv,
                capacityMl = item.capacityMl,
                levelStatus = item.levelStatus,
                purchasePrice = item.purchasePrice,
                isOpened = item.isOpened,
                openedAt = item.openedAt,
                expiryStatus = item.getExpiryStatus(now),
                dDay = item.openedAt?.let { -ChronoUnit.DAYS.between(it.toLocalDate(), now.toLocalDate()) },
            )
        }
    }
}

data class InventoryItemDetailResponse(
    val id: Long,
    val name: String,
    val category: Category,
    val abv: BigDecimal?,
    val capacityMl: Int?,
    val levelStatus: LevelStatus,
    val purchasePrice: Int?,
    val isOpened: Boolean,
    val openedAt: LocalDateTime?,
    val expiryStatus: ExpiryStatus,
    val dDay: Long?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(item: InventoryItem): InventoryItemDetailResponse {
            val now = LocalDateTime.now()
            return InventoryItemDetailResponse(
                id = item.id,
                name = item.name,
                category = item.category,
                abv = item.abv,
                capacityMl = item.capacityMl,
                levelStatus = item.levelStatus,
                purchasePrice = item.purchasePrice,
                isOpened = item.isOpened,
                openedAt = item.openedAt,
                expiryStatus = item.getExpiryStatus(now),
                dDay = item.openedAt?.let { -ChronoUnit.DAYS.between(it.toLocalDate(), now.toLocalDate()) },
                createdAt = item.createdAt,
                updatedAt = item.updatedAt,
            )
        }
    }
}

data class InventoryCategoryCountResponse(
    val category: Category,
    val count: Long,
    val percentage: Double,
)

data class InventoryInsightSummaryResponse(
    val totalValue: Long,
    val totalItemCount: Long,
    val availableRecipeCount: Int,
    val expiryWarningCount: Int,
)

data class InventoryPageResponse(
    val content: List<InventoryItemSummaryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
) {
    companion object {
        fun from(pageData: Page<InventoryItem>) = InventoryPageResponse(
            content = pageData.content.map(InventoryItemSummaryResponse::from),
            page = pageData.number,
            size = pageData.size,
            totalElements = pageData.totalElements,
            totalPages = pageData.totalPages,
            last = pageData.isLast,
        )
    }
}

data class InventoryHomeResponse(
    val insights: InventoryInsightSummaryResponse,
    val categoryCount: Map<String, Long>,
    val inventory: InventoryPageResponse,
)

data class InventoryListResponse(
    val content: List<InventoryItemSummaryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
) {
    companion object {
        fun from(pageData: Page<InventoryItem>) = InventoryListResponse(
            content = pageData.content.map(InventoryItemSummaryResponse::from),
            page = pageData.number,
            size = pageData.size,
            totalElements = pageData.totalElements,
            totalPages = pageData.totalPages,
            last = pageData.isLast,
        )
    }
}

data class InventoryScanResponse(
    val isMatchFound: Boolean,
)

data class ExpiryWarningItemResponse(
    val id: Long,
    val name: String,
    val openedAt: LocalDateTime,
    val dDay: Long,
    val expiryStatus: ExpiryStatus,
)

data class InventoryInsightsResponse(
    val totalValue: Long,
    val totalItemCount: Long,
    val availableRecipeCount: Int,
    val expiryWarningCount: Int,
    val categoryBreakdown: List<InventoryCategoryCountResponse>,
    val expiryWarningItems: List<ExpiryWarningItemResponse>,
)
