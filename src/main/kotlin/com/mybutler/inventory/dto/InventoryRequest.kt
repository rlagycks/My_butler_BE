package com.mybutler.inventory.dto

import com.mybutler.inventory.entity.Category
import com.mybutler.inventory.entity.LevelStatus
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateInventoryItemRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val name: String,

    @field:NotNull
    val category: Category,

    @field:DecimalMin("0.0")
    @field:DecimalMax("100.0")
    val abv: BigDecimal? = null,

    @field:Min(1)
    val capacityMl: Int? = null,

    @field:NotNull
    val levelStatus: LevelStatus,

    @field:Min(0)
    val purchasePrice: Int? = null,

    val isOpened: Boolean = false,

    val openedAt: LocalDateTime? = null,
)

data class UpdateInventoryItemRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val name: String,

    @field:NotNull
    val category: Category,

    @field:DecimalMin("0.0")
    @field:DecimalMax("100.0")
    val abv: BigDecimal? = null,

    @field:Min(1)
    val capacityMl: Int? = null,

    @field:NotNull
    val levelStatus: LevelStatus,

    @field:Min(0)
    val purchasePrice: Int? = null,

    val isOpened: Boolean = false,

    val openedAt: LocalDateTime? = null,
)

data class UpdateInventoryLevelRequest(
    @field:NotNull
    val levelStatus: LevelStatus,
)

data class ScanInventoryRequest(
    @field:Size(max = 5_000)
    val ocrText: String? = null,
)
