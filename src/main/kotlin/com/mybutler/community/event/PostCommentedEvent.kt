package com.mybutler.community.event

data class PostCommentedEvent(
    val postId: Long,
    val postAuthorId: Long,
    val actorUserId: Long,
)
