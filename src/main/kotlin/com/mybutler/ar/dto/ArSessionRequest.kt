package com.mybutler.ar.dto

data class ArSessionRequest(
    val recipeId: Long,
    val rating: Int?,
    val caption: String?,
)
