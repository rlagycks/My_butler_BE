package com.mybutler.inventory.service

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.inventory.dto.CreateInventoryItemRequest
import com.mybutler.inventory.dto.ExpiryWarningItemResponse
import com.mybutler.inventory.dto.InventoryCategoryCountResponse
import com.mybutler.inventory.dto.InventoryHomeResponse
import com.mybutler.inventory.dto.InventoryInsightSummaryResponse
import com.mybutler.inventory.dto.InventoryInsightsResponse
import com.mybutler.inventory.dto.InventoryItemDetailResponse
import com.mybutler.inventory.dto.InventoryListResponse
import com.mybutler.inventory.dto.InventoryPageResponse
import com.mybutler.inventory.dto.InventoryScanResponse
import com.mybutler.inventory.dto.UpdateInventoryItemRequest
import com.mybutler.inventory.dto.UpdateInventoryLevelRequest
import com.mybutler.inventory.entity.Category
import com.mybutler.inventory.entity.ExpiryStatus
import com.mybutler.inventory.entity.InventoryItem
import com.mybutler.inventory.repository.InventoryItemRepository
import com.mybutler.recipe.service.BaseRecipeLoader
import com.mybutler.recipe.service.RecipeMatchingService
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class InventoryService(
    private val inventoryItemRepository: InventoryItemRepository,
    private val baseRecipeLoader: BaseRecipeLoader,
) {
    fun getHome(userId: Long, pageable: Pageable): InventoryHomeResponse {
        val allItems = inventoryItemRepository.findAllByUserId(userId)
        val pageData = inventoryItemRepository.findByUserId(userId, pageable)

        return InventoryHomeResponse(
            insights = createInsightSummary(allItems),
            categoryCount = createCategoryCount(allItems),
            inventory = InventoryPageResponse.from(pageData),
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
                openedAt = if (request.isOpened) LocalDateTime.now() else null,
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

        when {
            !item.isOpened && request.isOpened -> {
                item.isOpened = true
                item.openedAt = LocalDateTime.now()
            }
            item.isOpened && !request.isOpened -> {
                item.isOpened = false
                item.openedAt = null
            }
        }

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
        val now = LocalDateTime.now()

        val warningItems = items.filter {
            val status = it.getExpiryStatus(now)
            status == ExpiryStatus.WARNING || status == ExpiryStatus.DANGER
        }

        val availableRecipeCount = RecipeMatchingService.partition(baseRecipeLoader.loadAll(), items).first.size

        return InventoryInsightsResponse(
            totalValue = items.sumOf { (it.purchasePrice ?: 0).toLong() },
            totalItemCount = items.size.toLong(),
            availableRecipeCount = availableRecipeCount,
            expiryWarningCount = warningItems.size,
            categoryBreakdown = createCategoryBreakdown(items),
            expiryWarningItems = warningItems.mapNotNull { item ->
                val openedAt = item.openedAt ?: return@mapNotNull null
                ExpiryWarningItemResponse(
                    id = item.id,
                    name = item.name,
                    openedAt = openedAt,
                    dDay = -ChronoUnit.DAYS.between(openedAt.toLocalDate(), now.toLocalDate()),
                    expiryStatus = item.getExpiryStatus(now),
                )
            },
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

    private fun createInsightSummary(items: List<InventoryItem>): InventoryInsightSummaryResponse {
        val now = LocalDateTime.now()
        val expiryCounts = items.groupingBy { it.getExpiryStatus(now) }.eachCount()

        val availableRecipeCount = RecipeMatchingService.partition(baseRecipeLoader.loadAll(), items).first.size

        return InventoryInsightSummaryResponse(
            totalValue = items.sumOf { (it.purchasePrice ?: 0).toLong() },
            totalItemCount = items.size.toLong(),
            availableRecipeCount = availableRecipeCount,
            expiryWarningCount = (expiryCounts[ExpiryStatus.WARNING] ?: 0) + (expiryCounts[ExpiryStatus.DANGER] ?: 0),
        )
    }

    private fun createCategoryCount(items: List<InventoryItem>): Map<String, Long> {
        val counts = items.groupingBy { it.category }.eachCount()
        val result = mutableMapOf<String, Long>("ALL" to items.size.toLong())
        Category.entries.forEach { category ->
            result[category.name] = (counts[category] ?: 0).toLong()
        }
        return result
    }

    private fun createCategoryBreakdown(items: List<InventoryItem>): List<InventoryCategoryCountResponse> {
        val total = items.size.toLong()
        val counts = items.groupingBy { it.category }.eachCount()

        return Category.entries.map { category ->
            val count = (counts[category] ?: 0).toLong()
            InventoryCategoryCountResponse(
                category = category,
                count = count,
                percentage = if (total > 0) count.toDouble() / total * 100.0 else 0.0,
            )
        }
    }
}
