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
@Transactional(readOnly = true)
class PostLikeService(
    private val postRepository: PostRepository,
    private val postLikeRepository: PostLikeRepository,
) {
    @Transactional
    fun toggleLike(postId: Long, userId: Long): LikeResponse {
        val post = postRepository.findByIdForUpdate(postId)
            ?: throw BusinessException(ErrorCode.POST_NOT_FOUND)

        return if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            postLikeRepository.deleteByPostIdAndUserId(postId, userId)
            post.likeCount = postLikeRepository.countByPostId(postId).toInt()
            LikeResponse(liked = false, likeCount = post.likeCount)
        } else {
            postLikeRepository.save(PostLike(postId = postId, userId = userId))
            post.likeCount = postLikeRepository.countByPostId(postId).toInt()
            LikeResponse(liked = true, likeCount = post.likeCount)
        }
    }

    fun getLikeStatus(postId: Long, userId: Long): LikeResponse {
        val post = postRepository.findByIdOrNull(postId)
            ?: throw BusinessException(ErrorCode.POST_NOT_FOUND)
        val liked = postLikeRepository.existsByPostIdAndUserId(postId, userId)
        return LikeResponse(liked = liked, likeCount = post.likeCount)
    }
}
