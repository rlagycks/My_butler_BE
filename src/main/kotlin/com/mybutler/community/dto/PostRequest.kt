package com.mybutler.community.dto

import com.mybutler.community.entity.PostType

data class CreatePostRequest(
    val type: PostType,
    val caption: String? = null,
    val recipeId: Long? = null,
)

data class CreateCommentRequest(
    val content: String,
)
