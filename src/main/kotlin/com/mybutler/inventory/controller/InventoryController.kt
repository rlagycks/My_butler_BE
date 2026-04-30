package com.mybutler.inventory.controller

import com.mybutler.common.response.ApiResponse
import com.mybutler.common.security.CustomUserDetails
import com.mybutler.inventory.dto.CreateInventoryItemRequest
import com.mybutler.inventory.dto.InventoryHomeResponse
import com.mybutler.inventory.dto.InventoryInsightsResponse
import com.mybutler.inventory.dto.InventoryItemDetailResponse
import com.mybutler.inventory.dto.InventoryListResponse
import com.mybutler.inventory.dto.InventoryScanResponse
import com.mybutler.inventory.dto.ScanInventoryRequest
import com.mybutler.inventory.dto.UpdateInventoryItemRequest
import com.mybutler.inventory.dto.UpdateInventoryLevelRequest
import com.mybutler.inventory.entity.Category
import com.mybutler.inventory.service.InventoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Inventory", description = "내 주류 보관함 API")
@RestController
@RequestMapping("/api/v1/inventory")
@SecurityRequirement(name = "Bearer Authentication")
class InventoryController(
    private val inventoryService: InventoryService,
) {
    @Operation(summary = "My Bar 홈 조회")
    @GetMapping("/home")
    fun getHome(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ApiResponse<InventoryHomeResponse> {
        return ApiResponse.ok(inventoryService.getHome(userDetails.userId))
    }

    @Operation(summary = "재고 목록 조회")
    @GetMapping
    fun getInventoryItems(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestParam(required = false) category: Category?,
        @ParameterObject
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable,
    ): ApiResponse<InventoryListResponse> {
        return ApiResponse.ok(inventoryService.getInventoryItems(userDetails.userId, category, pageable))
    }

    @Operation(summary = "재고 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody request: CreateInventoryItemRequest,
    ): ApiResponse<InventoryItemDetailResponse> {
        return ApiResponse.ok(inventoryService.create(userDetails.userId, request))
    }

    @Operation(summary = "재고 상세 조회")
    @GetMapping("/{id}")
    fun getDetail(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable id: Long,
    ): ApiResponse<InventoryItemDetailResponse> {
        return ApiResponse.ok(inventoryService.getDetail(userDetails.userId, id))
    }

    @Operation(summary = "재고 수정")
    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateInventoryItemRequest,
    ): ApiResponse<InventoryItemDetailResponse> {
        return ApiResponse.ok(inventoryService.update(userDetails.userId, id, request))
    }

    @Operation(summary = "재고 삭제")
    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable id: Long,
    ): ApiResponse<Unit> {
        inventoryService.delete(userDetails.userId, id)
        return ApiResponse.ok()
    }

    @Operation(summary = "개봉 처리")
    @PatchMapping("/{id}/open")
    fun open(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable id: Long,
    ): ApiResponse<InventoryItemDetailResponse> {
        return ApiResponse.ok(inventoryService.open(userDetails.userId, id))
    }

    @Operation(summary = "잔량 업데이트")
    @PatchMapping("/{id}/level")
    fun updateLevel(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateInventoryLevelRequest,
    ): ApiResponse<InventoryItemDetailResponse> {
        return ApiResponse.ok(inventoryService.updateLevel(userDetails.userId, id, request))
    }

    @Operation(summary = "라벨 스캔", description = "MVP 단계에서는 항상 매칭 실패를 반환합니다.")
    @PostMapping("/scan")
    fun scan(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody(required = false) @Schema(hidden = true) request: ScanInventoryRequest?,
    ): ApiResponse<InventoryScanResponse> {
        return ApiResponse.ok(inventoryService.scan(userDetails.userId))
    }

    @Operation(summary = "인사이트 상세 조회")
    @GetMapping("/insights")
    fun getInsights(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ApiResponse<InventoryInsightsResponse> {
        return ApiResponse.ok(inventoryService.getInsights(userDetails.userId))
    }
}
