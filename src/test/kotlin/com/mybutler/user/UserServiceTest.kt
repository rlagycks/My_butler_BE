package com.mybutler.user

import com.mybutler.auth.entity.AgeGroup
import com.mybutler.auth.entity.User
import com.mybutler.auth.repository.UserRepository
import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.user.dto.SavePreferencesRequest
import com.mybutler.user.dto.UpdateProfileRequest
import com.mybutler.user.dto.UpdateUsernameRequest
import com.mybutler.user.entity.TastePreference
import com.mybutler.user.entity.UserPreference
import com.mybutler.user.repository.UserPreferenceRepository
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
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var userPreferenceRepository: UserPreferenceRepository

    private lateinit var userService: UserService

    private val testUser = User(
        id = 1L, email = "test@email.com", username = "testuser",
        password = "encoded", termsAgreed = true, privacyAgreed = true,
    )

    @BeforeEach
    fun setUp() {
        userService = UserService(userRepository, userPreferenceRepository)
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
}
