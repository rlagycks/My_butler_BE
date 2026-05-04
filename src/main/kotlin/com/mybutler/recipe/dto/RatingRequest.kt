package com.mybutler.recipe.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

data class RatingUpsertRequest(
    @field:Min(1) @field:Max(5)
    val score: Int,

    @field:Size(max = 500)
    val comment: String? = null,
)
