package com.mybutler.common.response

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
) {
    companion object {
        fun <T> ok(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)
        fun <T> ok(): ApiResponse<T> = ApiResponse(success = true)
    }
}
