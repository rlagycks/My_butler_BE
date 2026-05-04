package com.mybutler.notification.event

import com.mybutler.auth.repository.UserRepository
import com.mybutler.community.event.CommentRepliedEvent
import com.mybutler.community.event.PostCommentedEvent
import com.mybutler.community.event.PostLikedEvent
import com.mybutler.notification.entity.NotificationType
import com.mybutler.notification.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class NotificationEventListener(
    private val notificationService: NotificationService,
    private val userRepository: UserRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePostLiked(event: PostLikedEvent) {
        if (event.actorUserId == event.postAuthorId) return
        runCatching {
            val actorUsername = userRepository.findById(event.actorUserId).map { it.username }.orElse("알 수 없음")
            notificationService.createNotification(
                recipientUserId = event.postAuthorId,
                actorUserId = event.actorUserId,
                type = NotificationType.POST_LIKE,
                targetId = event.postId,
                message = "${actorUsername}님이 회원님의 게시물을 좋아합니다.",
            )
        }.onFailure { logger.error("Failed to create POST_LIKE notification for post {}", event.postId, it) }
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePostCommented(event: PostCommentedEvent) {
        if (event.actorUserId == event.postAuthorId) return
        runCatching {
            val actorUsername = userRepository.findById(event.actorUserId).map { it.username }.orElse("알 수 없음")
            notificationService.createNotification(
                recipientUserId = event.postAuthorId,
                actorUserId = event.actorUserId,
                type = NotificationType.POST_COMMENT,
                targetId = event.postId,
                message = "${actorUsername}님이 회원님의 게시물에 댓글을 남겼습니다.",
            )
        }.onFailure { logger.error("Failed to create POST_COMMENT notification for post {}", event.postId, it) }
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleCommentReplied(event: CommentRepliedEvent) {
        if (event.actorUserId == event.parentCommentAuthorId) return
        runCatching {
            val actorUsername = userRepository.findById(event.actorUserId).map { it.username }.orElse("알 수 없음")
            notificationService.createNotification(
                recipientUserId = event.parentCommentAuthorId,
                actorUserId = event.actorUserId,
                type = NotificationType.COMMENT_REPLY,
                targetId = event.postId,
                message = "${actorUsername}님이 회원님의 댓글에 답글을 남겼습니다.",
            )
        }.onFailure { logger.error("Failed to create COMMENT_REPLY notification for post {}", event.postId, it) }
    }
}
