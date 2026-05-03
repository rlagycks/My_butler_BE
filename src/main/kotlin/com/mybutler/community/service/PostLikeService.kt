package com.mybutler.community.service

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.community.dto.LikeResponse
import com.mybutler.community.entity.PostLike
import com.mybutler.community.repository.PostLikeRepository
import com.mybutler.community.repository.PostRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PostLikeService(
    private val postRepository: PostRepository,
    private val postLikeRepository: PostLikeRepository,
) {
    fun addLike(postId: Long, userId: Long): LikeResponse {
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            throw BusinessException(ErrorCode.POST_LIKE_ALREADY_EXISTS)
        }
        val post = postRepository.findByIdForUpdate(postId)
            ?: throw BusinessException(ErrorCode.POST_NOT_FOUND)
        post.likeCount++
        postLikeRepository.save(PostLike(postId = postId, userId = userId))
        return LikeResponse(postId = postId, likeCount = post.likeCount, isLiked = true)
    }

    fun removeLike(postId: Long, userId: Long): LikeResponse {
        val like = postLikeRepository.findByPostIdAndUserId(postId, userId)
            ?: throw BusinessException(ErrorCode.POST_LIKE_NOT_FOUND)
        val post = postRepository.findByIdForUpdate(postId)
            ?: throw BusinessException(ErrorCode.POST_NOT_FOUND)
        post.likeCount = maxOf(0, post.likeCount - 1)
        postLikeRepository.delete(like)
        return LikeResponse(postId = postId, likeCount = post.likeCount, isLiked = false)
    }
}
