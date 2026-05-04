package com.mybutler.notification

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.notification.entity.Notification
import com.mybutler.notification.entity.NotificationType
import com.mybutler.notification.repository.NotificationRepository
import com.mybutler.notification.service.NotificationService
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
import org.mockito.kotlin.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class NotificationServiceTest {

    @Mock lateinit var notificationRepository: NotificationRepository

    private lateinit var notificationService: NotificationService

    @BeforeEach
    fun setUp() {
        notificationService = NotificationService(notificationRepository)
    }

    @Test
    fun `getNotifications - 페이지네이션된 알림 목록 반환`() {
        val pageable = PageRequest.of(0, 20)
        val notifications = listOf(
            notification(id = 2L, type = NotificationType.POST_LIKE, targetId = 10L),
            notification(id = 1L, type = NotificationType.EXPIRY_WARNING, targetId = 5L),
        )
        given(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(1L, pageable))
            .willReturn(PageImpl(notifications, pageable, 2))

        val result = notificationService.getNotifications(1L, pageable)

        assertThat(result.content).hasSize(2)
        assertThat(result.totalElements).isEqualTo(2)
        assertThat(result.page).isEqualTo(0)
        assertThat(result.last).isTrue()
        assertThat(result.content[0].link?.screen).isEqualTo("POST_DETAIL")
        assertThat(result.content[1].link?.screen).isEqualTo("INVENTORY_DETAIL")
    }

    @Test
    fun `getUnreadCount - 읽지 않은 알림 수 반환`() {
        given(notificationRepository.countByRecipientUserIdAndIsReadFalse(1L)).willReturn(5L)

        val result = notificationService.getUnreadCount(1L)

        assertThat(result.unreadCount).isEqualTo(5L)
    }

    @Test
    fun `markAsRead - 알림 읽음 처리 성공`() {
        val noti = notification(id = 1L, userId = 1L, isRead = false)
        given(notificationRepository.findById(1L)).willReturn(Optional.of(noti))

        val result = notificationService.markAsRead(1L, 1L)

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.isRead).isTrue()
        assertThat(noti.isRead).isTrue()
        assertThat(noti.readAt).isNotNull()
    }

    @Test
    fun `markAsRead - 알림 없을 때 NOTIFICATION_NOT_FOUND`() {
        given(notificationRepository.findById(999L)).willReturn(Optional.empty())

        val ex = assertThrows<BusinessException> {
            notificationService.markAsRead(999L, 1L)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND)
    }

    @Test
    fun `markAsRead - 다른 사용자 알림 접근 시 NOTIFICATION_ACCESS_DENIED`() {
        val noti = notification(id = 1L, userId = 1L, isRead = false)
        given(notificationRepository.findById(1L)).willReturn(Optional.of(noti))

        val ex = assertThrows<BusinessException> {
            notificationService.markAsRead(1L, 99L)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.NOTIFICATION_ACCESS_DENIED)
    }

    @Test
    fun `markAsRead - 이미 읽음 처리된 알림 NOTIFICATION_ALREADY_READ`() {
        val noti = notification(id = 1L, userId = 1L, isRead = true)
        given(notificationRepository.findById(1L)).willReturn(Optional.of(noti))

        val ex = assertThrows<BusinessException> {
            notificationService.markAsRead(1L, 1L)
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.NOTIFICATION_ALREADY_READ)
    }

    @Test
    fun `markAllAsRead - 전체 읽음 처리 후 업데이트 수 반환`() {
        given(notificationRepository.markAllAsRead(any(), any())).willReturn(3)

        val result = notificationService.markAllAsRead(1L)

        assertThat(result.updatedCount).isEqualTo(3)
    }

    @Test
    fun `markAllAsRead - 읽지 않은 알림 없을 때 0 반환`() {
        given(notificationRepository.markAllAsRead(any(), any())).willReturn(0)

        val result = notificationService.markAllAsRead(1L)

        assertThat(result.updatedCount).isEqualTo(0)
    }

    @Test
    fun `createNotification - 알림 저장`() {
        val captor = argumentCaptor<Notification>()
        given(notificationRepository.save(captor.capture())).willAnswer { captor.firstValue }

        notificationService.createNotification(
            recipientUserId = 1L,
            actorUserId = 2L,
            type = NotificationType.POST_LIKE,
            targetId = 10L,
            message = "테스트님이 회원님의 게시물을 좋아합니다.",
        )

        verify(notificationRepository).save(any())
        val saved = captor.firstValue
        assertThat(saved.recipientUserId).isEqualTo(1L)
        assertThat(saved.actorUserId).isEqualTo(2L)
        assertThat(saved.type).isEqualTo(NotificationType.POST_LIKE)
        assertThat(saved.targetId).isEqualTo(10L)
        assertThat(saved.isRead).isFalse()
    }

    @Test
    fun `createNotification - actorUserId 없이 만료 알림 저장`() {
        val captor = argumentCaptor<Notification>()
        given(notificationRepository.save(captor.capture())).willAnswer { captor.firstValue }

        notificationService.createNotification(
            recipientUserId = 1L,
            type = NotificationType.EXPIRY_WARNING,
            targetId = 5L,
            message = "개봉 후 7일이 경과했습니다.",
        )

        val saved = captor.firstValue
        assertThat(saved.actorUserId).isNull()
        assertThat(saved.type).isEqualTo(NotificationType.EXPIRY_WARNING)
    }

    private fun notification(
        id: Long = 1L,
        userId: Long = 1L,
        type: NotificationType = NotificationType.POST_LIKE,
        targetId: Long? = 10L,
        isRead: Boolean = false,
    ) = Notification(
        id = id,
        recipientUserId = userId,
        actorUserId = 2L,
        type = type,
        targetId = targetId,
        message = "테스트 알림",
        isRead = isRead,
        readAt = if (isRead) LocalDateTime.now() else null,
    )
}
