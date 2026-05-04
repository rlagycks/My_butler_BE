package com.mybutler.common.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.multipart.support.MissingServletRequestPartException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `photo 파트 누락 시 AR_PHOTO_REQUIRED 반환`() {
        val response = handler.handleMissingServletRequestPartException(MissingServletRequestPartException("photo"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body).isEqualTo(ErrorResponse.of(ErrorCode.AR_PHOTO_REQUIRED))
    }

    @Test
    fun `기타 파트 누락 시 INVALID_INPUT 반환`() {
        val response = handler.handleMissingServletRequestPartException(MissingServletRequestPartException("request"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body).isEqualTo(ErrorResponse.of(ErrorCode.INVALID_INPUT))
    }
}
