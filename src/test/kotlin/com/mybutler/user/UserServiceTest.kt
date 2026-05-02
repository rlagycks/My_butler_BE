package com.mybutler.user

import com.mybutler.auth.entity.AgeGroup
import com.mybutler.auth.entity.User
import com.mybutler.auth.repository.UserRepository
import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.user.dto.SavePreferencesRequest
import com.mybutler.user.dto.UpdateProfileRequest
import com.mybutler.user.dto.UpdateUsernameRequest
import com.mybutler.common.util.ImageUploadValidator
import com.mybutler.user.entity.TastePreference
import com.mybutler.user.entity.UserPreference
import com.mybutler.user.repository.UserPreferenceRepository
import com.mybutler.common.storage.StorageService
import com.mybutler.user.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.mock.web.MockMultipartFile
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var userPreferenceRepository: UserPreferenceRepository
    @Mock lateinit var storageService: StorageService

    private lateinit var userService: UserService
    private val imageUploadValidator = ImageUploadValidator()

    private val testUser = User(
        id = 1L, email = "test@email.com", username = "testuser",
        password = "encoded", termsAgreed = true, privacyAgreed = true,
    )

    @BeforeEach
    fun setUp() {
        userService = UserService(userRepository, userPreferenceRepository, storageService, imageUploadValidator)
    }

    @Test
    fun `getMyProfile - 존재하는 유저 프로필 반환`() {
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser))

        val result = userService.getMyProfile(1L)

        assertThat(result.email).isEqualTo("test@email.com")
        assertThat(result.username).isEqualTo("testuser")
    }

    @Test
    fun `getMyProfile - 존재하지 않는 유저 USER_NOT_FOUND 예외`() {
        given(userRepository.findById(any())).willReturn(Optional.empty())

        assertThatThrownBy { userService.getMyProfile(999L) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND)
    }

    @Test
    fun `savePreferences - 취향 저장 후 ageGroup 있으면 온보딩 완료`() {
        val user = User(id = 1L, email = "test@email.com", username = "testuser",
            password = "encoded", termsAgreed = true, privacyAgreed = true, ageGroup = AgeGroup.TWENTIES)
        val savedPref = UserPreference(id = 1L, userId = 1L,
            tastePreferences = mutableSetOf(TastePreference.SWEET))

        given(userRepository.findById(1L)).willReturn(Optional.of(user))
        given(userPreferenceRepository.findByUserId(1L)).willReturn(Optional.empty())
        given(userPreferenceRepository.save(any<UserPreference>())).willReturn(savedPref)
        given(userPreferenceRepository.existsByUserId(1L)).willReturn(true)

        val request = SavePreferencesRequest(
            tastePreferences = setOf(TastePreference.SWEET),
            preferredAbv = null,
            experienceLevel = null,
        )
        userService.savePreferences(1L, request)

        assertThat(user.onboardingCompleted).isTrue()
    }

    @Test
    fun `updateUsername - 중복 아이디 DUPLICATE_USERNAME 예외`() {
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser))
        given(userRepository.existsByUsername("taken")).willReturn(true)

        assertThatThrownBy { userService.updateUsername(1L, UpdateUsernameRequest("taken")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_USERNAME)
    }

    @Test
    fun `uploadProfileImage - 기존 이미지 없을 때 업로드 후 URL 반환`() {
        val file = MockMultipartFile("file", "photo.jpg", "image/jpeg", "data".toByteArray())
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser))
        given(storageService.upload(any(), any())).willReturn("profiles/uuid.jpg")

        val result = userService.uploadProfileImage(1L, file)

        assertThat(result.profileImageUrl).isEqualTo("profiles/uuid.jpg")
        verify(storageService).upload(file, "profiles")
    }

    @Test
    fun `uploadProfileImage - 기존 이미지 있으면 새 업로드 후 이전 파일 삭제`() {
        val userWithImage = User(
            id = 1L, email = "test@email.com", username = "testuser",
            password = "encoded", termsAgreed = true, privacyAgreed = true,
            profileImageUrl = "profiles/old.jpg",
        )
        val file = MockMultipartFile("file", "new.jpg", "image/jpeg", "data".toByteArray())
        given(userRepository.findById(1L)).willReturn(Optional.of(userWithImage))
        given(storageService.upload(any(), any())).willReturn("profiles/new-uuid.jpg")

        userService.uploadProfileImage(1L, file)

        inOrder(storageService) {
            verify(storageService).upload(file, "profiles")
            verify(storageService).delete("profiles/old.jpg")
        }
    }

    @Test
    fun `uploadProfileImage - 존재하지 않는 유저 USER_NOT_FOUND 예외`() {
        given(userRepository.findById(any())).willReturn(Optional.empty())
        val file = MockMultipartFile("file", "photo.jpg", "image/jpeg", "data".toByteArray())

        assertThatThrownBy { userService.uploadProfileImage(999L, file) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND)
    }

    @Test
    fun `uploadProfileImage - 이미지가 아니면 INVALID_FILE_TYPE 예외`() {
        val file = MockMultipartFile("file", "note.txt", "text/plain", "data".toByteArray())
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser))

        assertThatThrownBy { userService.uploadProfileImage(1L, file) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE_TYPE)

        verify(storageService, never()).upload(any(), any())
    }
}
