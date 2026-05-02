package com.mybutler.community.service

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.community.dto.CreatePostRequest
import com.mybutler.community.dto.PostPageResponse
import com.mybutler.community.dto.PostResponse
import com.mybutler.community.dto.UpdatePostRequest
import com.mybutler.community.entity.Post
import com.mybutler.community.entity.PostImage
import com.mybutler.community.repository.PostRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
) {
    @Transactional
    fun createPost(userId: Long, request: CreatePostRequest): PostResponse {
        val post = Post(userId = userId, title = request.title, content = request.content)
        replaceImages(post, request.imageUrls)
        return PostResponse.from(postRepository.save(post))
    }

    fun getFeed(pageable: Pageable): PostPageResponse {
        return PostPageResponse.from(postRepository.findAll(pageable))
    }

    fun getPostDetail(postId: Long): PostResponse {
        val post = postRepository.findByIdOrNull(postId)
            ?: throw BusinessException(ErrorCode.POST_NOT_FOUND)
        return PostResponse.from(post)
    }

    @Transactional
    fun updatePost(postId: Long, userId: Long, request: UpdatePostRequest): PostResponse {
        val post = findOwned(postId, userId)
        post.title = request.title
        post.content = request.content
        replaceImages(post, request.imageUrls)
        return PostResponse.from(post)
    }

    @Transactional
    fun deletePost(postId: Long, userId: Long) {
        postRepository.delete(findOwned(postId, userId))
    }

    fun getMyPosts(userId: Long, pageable: Pageable): PostPageResponse {
        return PostPageResponse.from(postRepository.findByUserId(userId, pageable))
    }

    private fun findOwned(postId: Long, userId: Long): Post {
        val post = postRepository.findByIdOrNull(postId)
            ?: throw BusinessException(ErrorCode.POST_NOT_FOUND)
        if (post.userId != userId) {
            throw BusinessException(ErrorCode.POST_AUTHOR_MISMATCH)
        }
        return post
    }

    private fun replaceImages(post: Post, imageUrls: List<String>) {
        post.images.clear()
        imageUrls.forEachIndexed { index, url ->
            post.images.add(PostImage(post = post, imageUrl = url, displayOrder = index))
        }
    }
}
