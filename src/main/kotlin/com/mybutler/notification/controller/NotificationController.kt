package com.mybutler.notification.controller

import com.mybutler.common.response.ApiResponse
import com.mybutler.common.security.CustomUserDetails
import com.mybutler.notification.dto.NotificationPageResponse
import com.mybutler.notification.dto.ReadAllNotificationResponse
import com.mybutler.notification.dto.ReadNotificationResponse
import com.mybutler.notification.dto.UnreadCountResponse
import com.mybutler.notification.service.NotificationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequestMapping("/api/v1/notifications")
@SecurityRequirement(name = "Bearer Authentication")
class NotificationController(
    private val notificationService: NotificationService,
) {
    @Operation(summary = "알림 목록 조회")
    @GetMapping
    fun getNotifications(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @ParameterObject @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<NotificationPageResponse> {
        return ApiResponse.ok(notificationService.getNotifications(userDetails.userId, pageable))
    }

    @Operation(summary = "읽지 않은 알림 수 조회")
    @GetMapping("/unread-count")
    fun getUnreadCount(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ApiResponse<UnreadCountResponse> {
        return ApiResponse.ok(notificationService.getUnreadCount(userDetails.userId))
    }

    @Operation(summary = "알림 읽음 처리")
    @PatchMapping("/{id}/read")
    fun markAsRead(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ApiResponse<ReadNotificationResponse> {
        return ApiResponse.ok(notificationService.markAsRead(id, userDetails.userId))
    }

    @Operation(summary = "알림 전체 읽음 처리")
    @PatchMapping("/read-all")
    fun markAllAsRead(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): ApiResponse<ReadAllNotificationResponse> {
        return ApiResponse.ok(notificationService.markAllAsRead(userDetails.userId))
    }
}
