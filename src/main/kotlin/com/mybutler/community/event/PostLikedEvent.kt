package com.mybutler.community.event

data class PostLikedEvent(
    val postId: Long,
    val postAuthorId: Long,
    val actorUserId: Long,
)
