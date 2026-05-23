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
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class InventoryService(
    private val inventoryItemRepository: InventoryItemRepository,
    private val baseRecipeLoader: BaseRecipeLoader,
    private val ocrClient: com.mybutler.inventory.ocr.OcrClient,
    private val labelParser: com.mybutler.inventory.ocr.LabelParser,
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
                tastingNotes = request.tastingNotes,
                purchasedAt = request.purchasedAt,
                purchasePlace = request.purchasePlace,
                origin = request.origin,
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
        item.tastingNotes = request.tastingNotes
        item.purchasedAt = request.purchasedAt
        item.purchasePlace = request.purchasePlace
        item.origin = request.origin

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

    /**
     * 라벨 OCR 스캔.
     * - ocrText가 직접 주어지면(개발/테스트) OCR을 건너뛰고 파서로 직행.
     * - 아니면 image 바이트를 OcrClient로 텍스트 추출 후 파싱.
     * 결과는 폼 prefill용이며 모든 필드 nullable.
     */
    fun scan(image: MultipartFile?, ocrText: String?): InventoryScanResponse {
        val rawText = when {
            !ocrText.isNullOrBlank() -> ocrText
            image != null && !image.isEmpty -> {
                // image.bytes는 IOException 가능 — OCR 처리 실패로 래핑하여 적절한 422 응답.
                val bytes = try {
                    image.bytes
                } catch (e: java.io.IOException) {
                    throw BusinessException(ErrorCode.INVENTORY_OCR_FAILED)
                }
                ocrClient.extractText(bytes)
            }
            else -> throw BusinessException(ErrorCode.INVALID_INPUT)
        }

        val parsed = labelParser.parse(rawText)
        return InventoryScanResponse(
            name = parsed.name,
            category = parsed.category,
            abv = parsed.abv,
            capacityMl = parsed.capacityMl,
            confidence = parsed.confidence,
            rawText = rawText,
        )
    }

    fun getInsights(userId: Long): InventoryInsightsResponse {
        val items = inventoryItemRepository.findAllByUserId(userId)
        val now = LocalDateTime.now()

        val warningItems = items.filter {
            val status = it.getExpiryStatus(now)
            status == ExpiryStatus.WARNING || status == ExpiryStatus.DANGER
        }

        val availableRecipeCount = countAvailableRecipes(items)

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

        return InventoryInsightSummaryResponse(
            totalValue = items.sumOf { (it.purchasePrice ?: 0).toLong() },
            totalItemCount = items.size.toLong(),
            availableRecipeCount = countAvailableRecipes(items),
            expiryWarningCount = (expiryCounts[ExpiryStatus.WARNING] ?: 0) + (expiryCounts[ExpiryStatus.DANGER] ?: 0),
        )
    }

    private fun countAvailableRecipes(items: List<InventoryItem>): Int {
        if (items.isEmpty()) {
            return 0
        }
        return RecipeMatchingService.partition(baseRecipeLoader.loadAll(), items).first.size
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
