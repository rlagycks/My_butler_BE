package com.mybutler.notification.scheduler

import com.mybutler.inventory.repository.InventoryItemRepository
import com.mybutler.notification.entity.NotificationType
import com.mybutler.notification.repository.NotificationRepository
import com.mybutler.notification.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Component
class ExpiryNotificationScheduler(
    private val inventoryItemRepository: InventoryItemRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationService: NotificationService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 6 * * *")
    fun sendExpiryNotifications() {
        val now = LocalDateTime.now()
        val todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT)
        val todayEnd = todayStart.plusDays(1)

        val openedItems = inventoryItemRepository.findAllByIsOpenedTrue()
        logger.info("Expiry notification scheduler started: {} opened items", openedItems.size)

        var sentCount = 0
        for (item in openedItems) {
            val openedDate = item.openedAt?.toLocalDate() ?: continue
            val elapsedDays = ChronoUnit.DAYS.between(openedDate, now.toLocalDate())

            val type = when {
                elapsedDays >= 14 -> NotificationType.EXPIRY_DANGER
                elapsedDays >= 7 -> NotificationType.EXPIRY_WARNING
                else -> continue
            }

            val alreadySent = notificationRepository.existsByTypeAndTargetIdAndCreatedAtBetween(
                type = type,
                targetId = item.id,
                start = todayStart,
                end = todayEnd,
            )
            if (alreadySent) continue

            val message = when (type) {
                NotificationType.EXPIRY_WARNING -> "${item.name} 개봉 후 7일이 경과했습니다. 빠른 시일 내에 소비하세요."
                NotificationType.EXPIRY_DANGER -> "${item.name} 개봉 후 14일이 경과했습니다. 품질이 저하될 수 있습니다."
                else -> continue
            }

            runCatching {
                notificationService.createNotification(
                    recipientUserId = item.userId,
                    type = type,
                    targetId = item.id,
                    message = message,
                )
                sentCount++
            }.onFailure { logger.error("Failed to create expiry notification for item {}", item.id, it) }
        }

        logger.info("Expiry notification scheduler completed: {} notifications sent", sentCount)
    }
}
