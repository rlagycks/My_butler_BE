package com.mybutler.common.email

interface EmailSender {
    fun send(to: String, subject: String, body: String)
}
