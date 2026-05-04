package com.mybutler.notification.dto

import com.mybutler.notification.entity.Notification
import com.mybutler.notification.entity.NotificationType
import java.time.LocalDateTime

data class NotificationLinkDto(
    val screen: String,
    val targetId: Long,
)

data class NotificationResponse(
    val id: Long,
    val type: NotificationType,
    val message: String,
    val isRead: Boolean,
    val link: NotificationLinkDto?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(notification: Notification): NotificationResponse {
            val screen = when (notification.type) {
                NotificationType.EXPIRY_WARNING, NotificationType.EXPIRY_DANGER -> "INVENTORY_DETAIL"
                NotificationType.POST_LIKE, NotificationType.POST_COMMENT, NotificationType.COMMENT_REPLY -> "POST_DETAIL"
            }
            val link = notification.targetId?.let { NotificationLinkDto(screen = screen, targetId = it) }
            return NotificationResponse(
                id = notification.id,
                type = notification.type,
                message = notification.message,
                isRead = notification.isRead,
                link = link,
                createdAt = notification.createdAt,
            )
        }
    }
}

data class NotificationPageResponse(
    val content: List<NotificationResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
)

data class UnreadCountResponse(val unreadCount: Long)

data class ReadNotificationResponse(val id: Long, val isRead: Boolean)

data class ReadAllNotificationResponse(val updatedCount: Int)
