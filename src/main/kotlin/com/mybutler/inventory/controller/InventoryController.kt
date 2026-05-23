package com.mybutler.inventory.controller

import com.mybutler.common.response.ApiResponse
import com.mybutler.common.security.CustomUserDetails
import com.mybutler.inventory.dto.CreateInventoryItemRequest
import com.mybutler.inventory.dto.InventoryHomeResponse
import com.mybutler.inventory.dto.InventoryInsightsResponse
import com.mybutler.inventory.dto.InventoryItemDetailResponse
import com.mybutler.inventory.dto.InventoryListResponse
import com.mybutler.inventory.dto.InventoryScanResponse
import com.mybutler.inventory.dto.UpdateInventoryItemRequest
import com.mybutler.inventory.dto.UpdateInventoryLevelRequest
import com.mybutler.inventory.entity.Category
import com.mybutler.inventory.service.InventoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

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
        @ParameterObject
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable,
    ): ApiResponse<InventoryHomeResponse> {
        return ApiResponse.ok(inventoryService.getHome(userDetails.userId, pageable))
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable id: Long,
    ) {
        inventoryService.delete(userDetails.userId, id)
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

    @Operation(
        summary = "라벨 OCR 스캔",
        description = "술병 라벨 이미지를 업로드하면 OCR로 제품명/카테고리/도수/용량을 추출합니다. " +
            "개발/테스트 시 image 대신 ocrText 폼 필드로 원시 텍스트를 직접 전달할 수 있습니다.",
    )
    @PostMapping("/scan", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun scan(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestPart("image", required = false) image: MultipartFile?,
        @RequestParam("ocrText", required = false) ocrText: String?,
    ): ApiResponse<InventoryScanResponse> {
        return ApiResponse.ok(inventoryService.scan(image, ocrText))
    }

    @Operation(summary = "인사이트 상세 조회")
    @GetMapping("/insights")
    fun getInsights(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ApiResponse<InventoryInsightsResponse> {
        return ApiResponse.ok(inventoryService.getInsights(userDetails.userId))
    }
}
