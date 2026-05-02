package com.mybutler.auth.service

import com.mybutler.auth.event.PasswordResetRequestedEvent
import com.mybutler.common.email.EmailSender
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PasswordResetEmailListener(
    private val emailSender: EmailSender,
    private val passwordResetEmailComposer: PasswordResetEmailComposer,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async("mailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: PasswordResetRequestedEvent) {
        runCatching {
            emailSender.send(
                to = event.email,
                subject = passwordResetEmailComposer.subject(),
                body = passwordResetEmailComposer.body(event.token),
            )
        }.onFailure { exception ->
            logger.error("Failed to send password reset email to {}", event.email, exception)
        }
    }
}
