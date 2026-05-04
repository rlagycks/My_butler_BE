package com.mybutler.community.event

data class CommentRepliedEvent(
    val postId: Long,
    val parentCommentAuthorId: Long,
    val actorUserId: Long,
)
