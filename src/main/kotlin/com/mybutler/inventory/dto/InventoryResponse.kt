package com.mybutler.inventory.dto

import com.mybutler.inventory.entity.Category
import com.mybutler.inventory.entity.ExpiryStatus
import com.mybutler.inventory.entity.InventoryItem
import com.mybutler.inventory.entity.LevelStatus
import org.springframework.data.domain.Page
import java.math.BigDecimal
import java.time.LocalDateTime

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
) {
    companion object {
        fun from(item: InventoryItem) = InventoryItemSummaryResponse(
            id = item.id,
            name = item.name,
            category = item.category,
            abv = item.abv,
            capacityMl = item.capacityMl,
            levelStatus = item.levelStatus,
            purchasePrice = item.purchasePrice,
            isOpened = item.isOpened,
            openedAt = item.openedAt,
            expiryStatus = item.getExpiryStatus(),
        )
    }
}

data class InventoryItemDetailResponse(
    val id: Long,
    val userId: Long,
    val name: String,
    val category: Category,
    val abv: BigDecimal?,
    val capacityMl: Int?,
    val levelStatus: LevelStatus,
    val purchasePrice: Int?,
    val isOpened: Boolean,
    val openedAt: LocalDateTime?,
    val expiryStatus: ExpiryStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(item: InventoryItem) = InventoryItemDetailResponse(
            id = item.id,
            userId = item.userId,
            name = item.name,
            category = item.category,
            abv = item.abv,
            capacityMl = item.capacityMl,
            levelStatus = item.levelStatus,
            purchasePrice = item.purchasePrice,
            isOpened = item.isOpened,
            openedAt = item.openedAt,
            expiryStatus = item.getExpiryStatus(),
            createdAt = item.createdAt,
            updatedAt = item.updatedAt,
        )
    }
}

data class InventoryCategoryCountResponse(
    val category: Category,
    val count: Long,
)

data class InventoryInsightBannerResponse(
    val totalCount: Int,
    val openedCount: Int,
    val expiringSoonCount: Int,
    val availableRecipeCount: Int,
)

data class InventoryHomeResponse(
    val insightBanner: InventoryInsightBannerResponse,
    val categoryCounts: List<InventoryCategoryCountResponse>,
    val items: List<InventoryItemSummaryResponse>,
)

data class InventoryListResponse(
    val items: List<InventoryItemSummaryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
) {
    companion object {
        fun from(pageData: Page<InventoryItem>) = InventoryListResponse(
            items = pageData.content.map(InventoryItemSummaryResponse::from),
            page = pageData.number,
            size = pageData.size,
            totalElements = pageData.totalElements,
            totalPages = pageData.totalPages,
            hasNext = pageData.hasNext(),
        )
    }
}

data class InventoryScanResponse(
    val isMatchFound: Boolean,
)

data class InventoryInsightsResponse(
    val totalCount: Int,
    val openedCount: Int,
    val unopenedCount: Int,
    val normalCount: Int,
    val warningCount: Int,
    val dangerCount: Int,
    val availableRecipeCount: Int,
    val categoryCounts: List<InventoryCategoryCountResponse>,
)
