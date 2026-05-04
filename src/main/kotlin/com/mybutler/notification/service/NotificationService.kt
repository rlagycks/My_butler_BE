package com.mybutler.notification.service

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.notification.dto.NotificationPageResponse
import com.mybutler.notification.dto.NotificationResponse
import com.mybutler.notification.dto.ReadAllNotificationResponse
import com.mybutler.notification.dto.ReadNotificationResponse
import com.mybutler.notification.dto.UnreadCountResponse
import com.mybutler.notification.entity.Notification
import com.mybutler.notification.entity.NotificationType
import com.mybutler.notification.repository.NotificationRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class NotificationService(
    private val notificationRepository: NotificationRepository,
) {
    fun getNotifications(userId: Long, pageable: Pageable): NotificationPageResponse {
        val page = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable)
        return NotificationPageResponse(
            content = page.content.map { NotificationResponse.from(it) },
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            last = page.isLast,
        )
    }

    fun getUnreadCount(userId: Long): UnreadCountResponse {
        return UnreadCountResponse(notificationRepository.countByRecipientUserIdAndIsReadFalse(userId))
    }

    @Transactional
    fun markAsRead(notificationId: Long, userId: Long): ReadNotificationResponse {
        val notification = notificationRepository.findByIdOrNull(notificationId)
            ?: throw BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND)
        if (notification.recipientUserId != userId) throw BusinessException(ErrorCode.NOTIFICATION_ACCESS_DENIED)
        if (notification.isRead) throw BusinessException(ErrorCode.NOTIFICATION_ALREADY_READ)

        notification.isRead = true
        notification.readAt = LocalDateTime.now()
        return ReadNotificationResponse(id = notification.id, isRead = true)
    }

    @Transactional
    fun markAllAsRead(userId: Long): ReadAllNotificationResponse {
        val updatedCount = notificationRepository.markAllAsRead(userId, LocalDateTime.now())
        return ReadAllNotificationResponse(updatedCount = updatedCount)
    }

    @Transactional
    fun createNotification(
        recipientUserId: Long,
        actorUserId: Long? = null,
        type: NotificationType,
        targetId: Long? = null,
        message: String,
    ) {
        notificationRepository.save(
            Notification(
                recipientUserId = recipientUserId,
                actorUserId = actorUserId,
                type = type,
                targetId = targetId,
                message = message,
            ),
        )
    }
}
