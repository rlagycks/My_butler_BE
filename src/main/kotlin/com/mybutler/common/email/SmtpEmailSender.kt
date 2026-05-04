package com.mybutler.common.email

import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
class SmtpEmailSender(
    private val mailSender: JavaMailSender,
    @Value("\${mail.from}") private val from: String,
) : EmailSender {

    override fun send(to: String, subject: String, body: String) {
        val message = SimpleMailMessage().apply {
            setFrom(from)
            setTo(to)
            setSubject(subject)
            text = body
        }
        mailSender.send(message)
    }
}
