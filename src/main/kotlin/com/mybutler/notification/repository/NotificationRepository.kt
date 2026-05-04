package com.mybutler.notification.repository

import com.mybutler.notification.entity.Notification
import com.mybutler.notification.entity.NotificationType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface NotificationRepository : JpaRepository<Notification, Long> {

    fun findByRecipientUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Notification>

    fun countByRecipientUserIdAndIsReadFalse(userId: Long): Long

    @Modifying
    @Query(
        "UPDATE Notification n SET n.isRead = true, n.readAt = :now " +
            "WHERE n.recipientUserId = :userId AND n.isRead = false",
    )
    fun markAllAsRead(userId: Long, now: LocalDateTime): Int

    fun existsByTypeAndTargetIdAndCreatedAtBetween(
        type: NotificationType,
        targetId: Long,
        start: LocalDateTime,
        end: LocalDateTime,
    ): Boolean
}
