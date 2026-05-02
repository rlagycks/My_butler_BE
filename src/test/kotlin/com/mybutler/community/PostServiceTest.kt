package com.mybutler.community

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.community.dto.CreatePostRequest
import com.mybutler.community.dto.UpdatePostRequest
import com.mybutler.community.entity.Post
import com.mybutler.community.repository.PostRepository
import com.mybutler.community.service.PostService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class PostServiceTest {

    @Mock lateinit var postRepository: PostRepository

    private lateinit var postService: PostService

    @BeforeEach
    fun setUp() {
        postService = PostService(postRepository)
    }

    @Test
    fun `createPost - 이미지 없이 게시글 생성`() {
        val savedPost = post(id = 1L, userId = 10L, title = "제목", content = "내용")
        given(postRepository.save(any<Post>())).willReturn(savedPost)

        val result = postService.createPost(10L, CreatePostRequest(title = "제목", content = "내용"))

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.title).isEqualTo("제목")
        assertThat(result.imageUrls).isEmpty()
    }

    @Test
    fun `createPost - 이미지 포함 게시글 생성`() {
        val savedPost = post(id = 2L, userId = 10L)
        given(postRepository.save(any<Post>())).willReturn(savedPost)

        val result = postService.createPost(
            10L,
            CreatePostRequest(title = "제목", content = "내용", imageUrls = listOf("https://example.com/a.jpg", "https://example.com/b.jpg"))
        )

        assertThat(result.imageUrls).hasSize(2)
        assertThat(result.thumbnailUrl).isEqualTo("https://example.com/a.jpg")
    }

    @Test
    fun `getPostDetail - 존재하는 게시글 반환`() {
        val p = post(id = 3L, userId = 10L, title = "조회 제목")
        given(postRepository.findById(3L)).willReturn(Optional.of(p))

        val result = postService.getPostDetail(3L)

        assertThat(result.title).isEqualTo("조회 제목")
    }

    @Test
    fun `getPostDetail - 없으면 POST_NOT_FOUND`() {
        given(postRepository.findById(999L)).willReturn(Optional.empty())

        val ex = assertThrows<BusinessException> { postService.getPostDetail(999L) }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.POST_NOT_FOUND)
    }

    @Test
    fun `updatePost - 작성자가 수정`() {
        val p = post(id = 4L, userId = 10L, title = "old", content = "old content")
        given(postRepository.findById(4L)).willReturn(Optional.of(p))

        val result = postService.updatePost(4L, 10L, UpdatePostRequest(title = "new", content = "new content"))

        assertThat(result.title).isEqualTo("new")
        assertThat(p.title).isEqualTo("new")
    }

    @Test
    fun `updatePost - 다른 사용자가 수정 시 POST_AUTHOR_MISMATCH`() {
        val p = post(id = 5L, userId = 10L)
        given(postRepository.findById(5L)).willReturn(Optional.of(p))

        val ex = assertThrows<BusinessException> {
            postService.updatePost(5L, 99L, UpdatePostRequest(title = "x", content = "x"))
        }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.POST_AUTHOR_MISMATCH)
    }

    @Test
    fun `deletePost - 작성자가 삭제`() {
        val p = post(id = 6L, userId = 10L)
        given(postRepository.findById(6L)).willReturn(Optional.of(p))

        postService.deletePost(6L, 10L)

        verify(postRepository).delete(p)
    }

    @Test
    fun `deletePost - 다른 사용자가 삭제 시 POST_AUTHOR_MISMATCH`() {
        val p = post(id = 7L, userId = 10L)
        given(postRepository.findById(7L)).willReturn(Optional.of(p))

        val ex = assertThrows<BusinessException> { postService.deletePost(7L, 99L) }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.POST_AUTHOR_MISMATCH)
    }

    @Test
    fun `getFeed - 페이징된 게시글 목록 반환`() {
        val pageable = PageRequest.of(0, 20)
        given(postRepository.findAll(pageable)).willReturn(
            PageImpl(listOf(post(id = 1L, userId = 1L), post(id = 2L, userId = 2L)), pageable, 2)
        )

        val result = postService.getFeed(pageable)

        assertThat(result.content).hasSize(2)
        assertThat(result.totalElements).isEqualTo(2)
    }

    @Test
    fun `getMyPosts - 내 게시글만 반환`() {
        val pageable = PageRequest.of(0, 20)
        given(postRepository.findByUserId(10L, pageable)).willReturn(
            PageImpl(listOf(post(id = 1L, userId = 10L)), pageable, 1)
        )

        val result = postService.getMyPosts(10L, pageable)

        assertThat(result.content).hasSize(1)
        assertThat(result.content.first().userId).isEqualTo(10L)
    }

    private fun post(
        id: Long = 1L,
        userId: Long = 1L,
        title: String = "Test Title",
        content: String = "Test content",
    ) = Post(id = id, userId = userId, title = title, content = content)
}
