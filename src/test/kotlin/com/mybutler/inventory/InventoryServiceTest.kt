package com.mybutler.inventory

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.inventory.dto.CreateInventoryItemRequest
import com.mybutler.inventory.dto.UpdateInventoryItemRequest
import com.mybutler.inventory.dto.UpdateInventoryLevelRequest
import com.mybutler.inventory.entity.Category
import com.mybutler.inventory.entity.ExpiryStatus
import com.mybutler.inventory.entity.InventoryItem
import com.mybutler.inventory.entity.LevelStatus
import com.mybutler.inventory.repository.InventoryItemRepository
import com.mybutler.inventory.service.InventoryService
import com.mybutler.recipe.service.BaseRecipeLoader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class InventoryServiceTest {

    @Mock lateinit var inventoryItemRepository: InventoryItemRepository
    @Mock lateinit var baseRecipeLoader: BaseRecipeLoader

    private lateinit var inventoryService: InventoryService

    @BeforeEach
    fun setUp() {
        inventoryService = InventoryService(inventoryItemRepository, baseRecipeLoader)
    }

    @Test
    fun `create - 미개봉 재고 생성 시 openedAt 없이 저장`() {
        val userId = 1L
        val request = CreateInventoryItemRequest(
            name = "Wild Turkey 101",
            category = Category.WHISKEY,
            abv = BigDecimal("50.5"),
            capacityMl = 700,
            levelStatus = LevelStatus.FULL,
            purchasePrice = 49000,
            isOpened = false,
        )
        val savedItem = inventoryItem(
            id = 100L,
            userId = userId,
            name = request.name,
            category = request.category,
            abv = request.abv,
            capacityMl = request.capacityMl,
            levelStatus = request.levelStatus,
            purchasePrice = request.purchasePrice,
            isOpened = false,
            openedAt = null,
        )
        val itemCaptor = argumentCaptor<InventoryItem>()

        given(inventoryItemRepository.save(any<InventoryItem>())).willReturn(savedItem)

        val result = inventoryService.create(userId, request)

        verify(inventoryItemRepository).save(itemCaptor.capture())
        assertThat(itemCaptor.firstValue.userId).isEqualTo(userId)
        assertThat(itemCaptor.firstValue.name).isEqualTo(request.name)
        assertThat(itemCaptor.firstValue.isOpened).isFalse()
        assertThat(itemCaptor.firstValue.openedAt).isNull()
        assertThat(result.id).isEqualTo(savedItem.id)
        assertThat(result.expiryStatus).isEqualTo(ExpiryStatus.UNOPENED)
    }

    @Test
    fun `create - 개봉 재고 생성 시 openedAt 자동 설정`() {
        val userId = 1L
        val request = CreateInventoryItemRequest(
            name = "Bombay Sapphire",
            category = Category.GIN,
            abv = BigDecimal("47.0"),
            capacityMl = 750,
            levelStatus = LevelStatus.HALF,
            purchasePrice = 42000,
            isOpened = true,
        )
        val savedItem = inventoryItem(
            id = 101L,
            userId = userId,
            name = request.name,
            category = request.category,
            abv = request.abv,
            capacityMl = request.capacityMl,
            levelStatus = request.levelStatus,
            purchasePrice = request.purchasePrice,
            isOpened = true,
            openedAt = LocalDateTime.now(),
        )
        val itemCaptor = argumentCaptor<InventoryItem>()

        given(inventoryItemRepository.save(any<InventoryItem>())).willReturn(savedItem)

        val result = inventoryService.create(userId, request)

        verify(inventoryItemRepository).save(itemCaptor.capture())
        assertThat(itemCaptor.firstValue.isOpened).isTrue()
        assertThat(itemCaptor.firstValue.openedAt).isNotNull()
        assertThat(result.isOpened).isTrue()
        assertThat(result.openedAt).isNotNull()
    }

    @Test
    fun `getDetail - 본인 재고 상세 반환`() {
        val userId = 1L
        val itemId = 10L
        val item = inventoryItem(
            id = itemId,
            userId = userId,
            name = "Bacardi Carta Blanca",
            category = Category.RUM,
            isOpened = true,
            openedAt = LocalDateTime.now().minusDays(15),
        )

        given(inventoryItemRepository.findById(itemId)).willReturn(Optional.of(item))

        val result = inventoryService.getDetail(userId, itemId)

        assertThat(result.id).isEqualTo(itemId)
        assertThat(result.name).isEqualTo(item.name)
        assertThat(result.category).isEqualTo(Category.RUM)
        assertThat(result.expiryStatus).isEqualTo(ExpiryStatus.NORMAL)
    }

    @Test
    fun `getDetail - 존재하지 않는 재고 INVENTORY_NOT_FOUND 예외`() {
        val userId = 1L
        val itemId = 999L

        given(inventoryItemRepository.findById(itemId)).willReturn(Optional.empty())

        val ex = assertThrows<BusinessException> {
            inventoryService.getDetail(userId, itemId)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.INVENTORY_NOT_FOUND)
    }

    @Test
    fun `getDetail - 다른 사용자의 재고 INVENTORY_ACCESS_DENIED 예외`() {
        val userId = 1L
        val itemId = 20L
        val item = inventoryItem(id = itemId, userId = 2L)

        given(inventoryItemRepository.findById(itemId)).willReturn(Optional.of(item))

        val ex = assertThrows<BusinessException> {
            inventoryService.getDetail(userId, itemId)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.INVENTORY_ACCESS_DENIED)
    }

    @Test
    fun `update - 요청 값으로 재고 수정 후 미개봉이면 openedAt 제거`() {
        val userId = 1L
        val itemId = 30L
        val item = inventoryItem(
            id = itemId,
            userId = userId,
            name = "Old Name",
            category = Category.OTHER,
            abv = BigDecimal("12.0"),
            capacityMl = 500,
            levelStatus = LevelStatus.LOW,
            purchasePrice = 10000,
            isOpened = true,
            openedAt = LocalDateTime.now().minusDays(3),
        )
        val request = UpdateInventoryItemRequest(
            name = "Patron Silver",
            category = Category.TEQUILA,
            abv = BigDecimal("40.0"),
            capacityMl = 700,
            levelStatus = LevelStatus.FULL,
            purchasePrice = 78000,
            isOpened = false,
        )

        given(inventoryItemRepository.findById(itemId)).willReturn(Optional.of(item))

        val result = inventoryService.update(userId, itemId, request)

        assertThat(item.name).isEqualTo(request.name)
        assertThat(item.category).isEqualTo(request.category)
        assertThat(item.abv).isEqualTo(request.abv)
        assertThat(item.capacityMl).isEqualTo(request.capacityMl)
        assertThat(item.levelStatus).isEqualTo(request.levelStatus)
        assertThat(item.purchasePrice).isEqualTo(request.purchasePrice)
        assertThat(item.isOpened).isFalse()
        assertThat(item.openedAt).isNull()
        assertThat(result.name).isEqualTo(request.name)
        assertThat(result.expiryStatus).isEqualTo(ExpiryStatus.UNOPENED)
    }

    @Test
    fun `open - 미개봉 재고를 개봉 처리`() {
        val userId = 1L
        val itemId = 40L
        val item = inventoryItem(
            id = itemId,
            userId = userId,
            isOpened = false,
            openedAt = null,
        )
        val before = LocalDateTime.now().minusSeconds(1)

        given(inventoryItemRepository.findById(itemId)).willReturn(Optional.of(item))

        val result = inventoryService.open(userId, itemId)

        assertThat(item.isOpened).isTrue()
        assertThat(item.openedAt).isNotNull()
        assertThat(item.openedAt).isAfterOrEqualTo(before)
        assertThat(result.isOpened).isTrue()
        assertThat(result.openedAt).isEqualTo(item.openedAt)
        assertThat(result.expiryStatus).isEqualTo(ExpiryStatus.DANGER)
    }

    @Test
    fun `open - 이미 개봉한 재고 INVENTORY_ALREADY_OPENED 예외`() {
        val userId = 1L
        val itemId = 41L
        val item = inventoryItem(
            id = itemId,
            userId = userId,
            isOpened = true,
            openedAt = LocalDateTime.now().minusDays(1),
        )

        given(inventoryItemRepository.findById(itemId)).willReturn(Optional.of(item))

        val ex = assertThrows<BusinessException> {
            inventoryService.open(userId, itemId)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.INVENTORY_ALREADY_OPENED)
    }

    @Test
    fun `updateLevel - 재고 잔량 상태 수정`() {
        val userId = 1L
        val itemId = 50L
        val item = inventoryItem(
            id = itemId,
            userId = userId,
            levelStatus = LevelStatus.FULL,
        )
        val request = UpdateInventoryLevelRequest(levelStatus = LevelStatus.LOW)

        given(inventoryItemRepository.findById(itemId)).willReturn(Optional.of(item))

        val result = inventoryService.updateLevel(userId, itemId, request)

        assertThat(item.levelStatus).isEqualTo(LevelStatus.LOW)
        assertThat(result.levelStatus).isEqualTo(LevelStatus.LOW)
    }

    @Test
    fun `scan - 현재 항상 미매칭 반환`() {
        val result = inventoryService.scan(userId = 1L)

        assertThat(result.isMatchFound).isFalse()
    }

    @Test
    fun `getInsights - 만료 상태와 카테고리 수량 집계 반환`() {
        val userId = 1L
        val now = LocalDateTime.now()
        val dangerItem = inventoryItem(
            id = 60L,
            userId = userId,
            category = Category.WHISKEY,
            isOpened = true,
            openedAt = now.minusDays(3),
        )
        val warningItem = inventoryItem(
            id = 61L,
            userId = userId,
            category = Category.WHISKEY,
            isOpened = true,
            openedAt = now.minusDays(10),
        )
        val normalItem = inventoryItem(
            id = 62L,
            userId = userId,
            category = Category.GIN,
            isOpened = true,
            openedAt = now.minusDays(20),
        )
        val unopenedItem = inventoryItem(
            id = 63L,
            userId = userId,
            category = Category.RUM,
            isOpened = false,
            openedAt = null,
        )

        assertThat(dangerItem.getExpiryStatus(now)).isEqualTo(ExpiryStatus.DANGER)
        assertThat(warningItem.getExpiryStatus(now)).isEqualTo(ExpiryStatus.WARNING)
        assertThat(normalItem.getExpiryStatus(now)).isEqualTo(ExpiryStatus.NORMAL)
        assertThat(unopenedItem.getExpiryStatus(now)).isEqualTo(ExpiryStatus.UNOPENED)
        given(inventoryItemRepository.findAllByUserId(userId)).willReturn(
            listOf(dangerItem, warningItem, normalItem, unopenedItem)
        )
        given(baseRecipeLoader.loadAll()).willReturn(emptyList())

        val result = inventoryService.getInsights(userId)

        assertThat(result.totalItemCount).isEqualTo(4)
        assertThat(result.expiryWarningCount).isEqualTo(2)
        assertThat(result.availableRecipeCount).isEqualTo(0)
        assertThat(result.categoryBreakdown.associate { it.category to it.count }).containsEntry(Category.WHISKEY, 2L)
        assertThat(result.categoryBreakdown.associate { it.category to it.count }).containsEntry(Category.GIN, 1L)
        assertThat(result.categoryBreakdown.associate { it.category to it.count }).containsEntry(Category.RUM, 1L)
        assertThat(result.expiryWarningItems).hasSize(2)
    }

    @Test
    fun `getHome - 재고가 비어 있으면 레시피 로더를 호출하지 않음`() {
        val userId = 1L
        val pageable = PageRequest.of(0, 20)
        given(inventoryItemRepository.findAllByUserId(userId)).willReturn(emptyList())
        given(inventoryItemRepository.findByUserId(userId, pageable)).willReturn(PageImpl(emptyList(), pageable, 0))

        val result = inventoryService.getHome(userId, pageable)

        assertThat(result.insights.availableRecipeCount).isEqualTo(0)
        verify(baseRecipeLoader, never()).loadAll()
    }

    private fun inventoryItem(
        id: Long = 1L,
        userId: Long = 1L,
        name: String = "Jameson",
        category: Category = Category.WHISKEY,
        abv: BigDecimal? = BigDecimal("40.0"),
        capacityMl: Int? = 700,
        levelStatus: LevelStatus = LevelStatus.FULL,
        purchasePrice: Int? = 39000,
        isOpened: Boolean = false,
        openedAt: LocalDateTime? = null,
    ): InventoryItem {
        return InventoryItem(
            id = id,
            userId = userId,
            name = name,
            category = category,
            abv = abv,
            capacityMl = capacityMl,
            levelStatus = levelStatus,
            purchasePrice = purchasePrice,
            isOpened = isOpened,
            openedAt = openedAt,
        )
    }
}
