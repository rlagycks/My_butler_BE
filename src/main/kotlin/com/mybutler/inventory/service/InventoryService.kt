package com.mybutler.inventory.service

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.inventory.dto.CreateInventoryItemRequest
import com.mybutler.inventory.dto.InventoryCategoryCountResponse
import com.mybutler.inventory.dto.InventoryHomeResponse
import com.mybutler.inventory.dto.InventoryInsightBannerResponse
import com.mybutler.inventory.dto.InventoryInsightsResponse
import com.mybutler.inventory.dto.InventoryItemDetailResponse
import com.mybutler.inventory.dto.InventoryListResponse
import com.mybutler.inventory.dto.InventoryScanResponse
import com.mybutler.inventory.dto.UpdateInventoryItemRequest
import com.mybutler.inventory.dto.UpdateInventoryLevelRequest
import com.mybutler.inventory.entity.Category
import com.mybutler.inventory.entity.ExpiryStatus
import com.mybutler.inventory.entity.InventoryItem
import com.mybutler.inventory.repository.InventoryItemRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class InventoryService(
    private val inventoryItemRepository: InventoryItemRepository,
) {
    fun getHome(userId: Long): InventoryHomeResponse {
        val items = inventoryItemRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)

        return InventoryHomeResponse(
            insightBanner = createInsightBanner(items),
            categoryCounts = createCategoryCounts(items),
            items = items.map(com.mybutler.inventory.dto.InventoryItemSummaryResponse::from),
        )
    }

    fun getInventoryItems(
        userId: Long,
        category: Category?,
        pageable: Pageable,
    ): InventoryListResponse {
        val pageData = if (category == null) {
            inventoryItemRepository.findByUserId(userId, pageable)
        } else {
            inventoryItemRepository.findByUserIdAndCategory(userId, category, pageable)
        }

        return InventoryListResponse.from(pageData)
    }

    @Transactional
    fun create(userId: Long, request: CreateInventoryItemRequest): InventoryItemDetailResponse {
        val saved = inventoryItemRepository.save(
            InventoryItem(
                userId = userId,
                name = request.name,
                category = request.category,
                abv = request.abv,
                capacityMl = request.capacityMl,
                levelStatus = request.levelStatus,
                purchasePrice = request.purchasePrice,
                isOpened = request.isOpened,
                openedAt = if (request.isOpened) request.openedAt else null,
            )
        )

        return InventoryItemDetailResponse.from(saved)
    }

    fun getDetail(userId: Long, inventoryItemId: Long): InventoryItemDetailResponse {
        return InventoryItemDetailResponse.from(findOwned(userId, inventoryItemId))
    }

    @Transactional
    fun update(
        userId: Long,
        inventoryItemId: Long,
        request: UpdateInventoryItemRequest,
    ): InventoryItemDetailResponse {
        val item = findOwned(userId, inventoryItemId)
        item.name = request.name
        item.category = request.category
        item.abv = request.abv
        item.capacityMl = request.capacityMl
        item.levelStatus = request.levelStatus
        item.purchasePrice = request.purchasePrice
        item.isOpened = request.isOpened
        item.openedAt = if (request.isOpened) request.openedAt else null

        return InventoryItemDetailResponse.from(item)
    }

    @Transactional
    fun delete(userId: Long, inventoryItemId: Long) {
        inventoryItemRepository.delete(findOwned(userId, inventoryItemId))
    }

    @Transactional
    fun open(userId: Long, inventoryItemId: Long): InventoryItemDetailResponse {
        val item = findOwned(userId, inventoryItemId)

        if (item.isOpened) {
            throw BusinessException(ErrorCode.INVENTORY_ALREADY_OPENED)
        }

        item.isOpened = true
        item.openedAt = LocalDateTime.now()

        return InventoryItemDetailResponse.from(item)
    }

    @Transactional
    fun updateLevel(
        userId: Long,
        inventoryItemId: Long,
        request: UpdateInventoryLevelRequest,
    ): InventoryItemDetailResponse {
        val item = findOwned(userId, inventoryItemId)
        item.levelStatus = request.levelStatus

        return InventoryItemDetailResponse.from(item)
    }

    fun scan(userId: Long): InventoryScanResponse {
        return InventoryScanResponse(isMatchFound = false)
    }

    fun getInsights(userId: Long): InventoryInsightsResponse {
        val items = inventoryItemRepository.findAllByUserId(userId)
        val expiryCounts = items.groupingBy { it.getExpiryStatus() }.eachCount()

        return InventoryInsightsResponse(
            totalCount = items.size,
            openedCount = items.count { it.isOpened },
            unopenedCount = items.count { !it.isOpened },
            normalCount = expiryCounts[ExpiryStatus.NORMAL] ?: 0,
            warningCount = expiryCounts[ExpiryStatus.WARNING] ?: 0,
            dangerCount = expiryCounts[ExpiryStatus.DANGER] ?: 0,
            availableRecipeCount = 0,
            categoryCounts = createCategoryCounts(items),
        )
    }

    private fun findOwned(userId: Long, inventoryItemId: Long): InventoryItem {
        val item = inventoryItemRepository.findByIdOrNull(inventoryItemId)
            ?: throw BusinessException(ErrorCode.INVENTORY_NOT_FOUND)

        if (item.userId != userId) {
            throw BusinessException(ErrorCode.INVENTORY_ACCESS_DENIED)
        }

        return item
    }

    private fun createInsightBanner(items: List<InventoryItem>): InventoryInsightBannerResponse {
        val expiryCounts = items.groupingBy { it.getExpiryStatus() }.eachCount()

        return InventoryInsightBannerResponse(
            totalCount = items.size,
            openedCount = items.count { it.isOpened },
            expiringSoonCount = (expiryCounts[ExpiryStatus.WARNING] ?: 0) + (expiryCounts[ExpiryStatus.DANGER] ?: 0),
            availableRecipeCount = 0,
        )
    }

    private fun createCategoryCounts(items: List<InventoryItem>): List<InventoryCategoryCountResponse> {
        val counts = items.groupingBy { it.category }.eachCount()

        return Category.entries.map { category ->
            InventoryCategoryCountResponse(
                category = category,
                count = (counts[category] ?: 0).toLong(),
            )
        }
    }
}
