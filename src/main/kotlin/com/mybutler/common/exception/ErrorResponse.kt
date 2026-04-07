package com.mybutler.common.exception

import org.springframework.validation.BindingResult

data class ErrorResponse(
    val code: String,
    val message: String,
    val errors: List<FieldError> = emptyList(),
) {
    data class FieldError(
        val field: String,
        val value: String,
        val reason: String,
    )

    companion object {
        fun of(errorCode: ErrorCode): ErrorResponse =
            ErrorResponse(code = errorCode.code, message = errorCode.message)

        fun of(errorCode: ErrorCode, errors: List<FieldError>): ErrorResponse =
            ErrorResponse(code = errorCode.code, message = errorCode.message, errors = errors)

        fun of(bindingResult: BindingResult): ErrorResponse =
            ErrorResponse(
                code = ErrorCode.INVALID_INPUT.code,
                message = ErrorCode.INVALID_INPUT.message,
                errors = bindingResult.fieldErrors.map {
                    FieldError(
                        field = it.field,
                        value = it.rejectedValue?.toString() ?: "",
                        reason = it.defaultMessage ?: "",
                    )
                },
            )
    }
}
