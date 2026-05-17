package com.mybutler.auth

import com.mybutler.auth.dto.LoginRequest
import com.mybutler.auth.dto.RegisterRequest
import com.mybutler.auth.event.PasswordResetRequestedEvent
import com.mybutler.auth.entity.RefreshToken
import com.mybutler.auth.entity.User
import com.mybutler.auth.repository.RefreshTokenRepository
import com.mybutler.auth.repository.UserRepository
import com.mybutler.auth.service.AuthService
import com.mybutler.auth.service.PasswordResetTokenStore
import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.common.security.JwtTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var refreshTokenRepository: RefreshTokenRepository
    @Mock lateinit var jwtTokenProvider: JwtTokenProvider
    @Mock lateinit var passwordResetTokenStore: PasswordResetTokenStore
    @Mock lateinit var applicationEventPublisher: ApplicationEventPublisher

    private val passwordEncoder = BCryptPasswordEncoder()
    private val refreshTokenExpiryMs = 604_800_000L

    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        authService = AuthService(
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            passwordEncoder = passwordEncoder,
            jwtTokenProvider = jwtTokenProvider,
            passwordResetTokenStore = passwordResetTokenStore,
            applicationEventPublisher = applicationEventPublisher,
            refreshTokenExpiryMs = refreshTokenExpiryMs,
        )
    }

    @Test
    fun `register - 정상 회원가입 시 토큰 반환`() {
        val request = RegisterRequest(
            email = "test@email.com",
            password = "Test1234!",
            termsAgreed = true,
            privacyAgreed = true,
            marketingAgreed = false,
        )
        val savedUser = User(id = 1L, email = request.email, username = "user_abcd1234",
            password = "encoded", termsAgreed = true, privacyAgreed = true)

        given(userRepository.existsByEmail(request.email)).willReturn(false)
        given(userRepository.save(any<User>())).willReturn(savedUser)
        given(jwtTokenProvider.createAccessToken(any(), any(), any())).willReturn("access-token")
        given(jwtTokenProvider.createRefreshToken()).willReturn("refresh-token")
        given(refreshTokenRepository.save(any<RefreshToken>())).willReturn(
            RefreshToken(userId = 1L, token = "refresh-token", expiresAt = LocalDateTime.now().plusDays(7))
        )

        val result = authService.register(request)

        assertThat(result.accessToken).isEqualTo("access-token")
        assertThat(result.refreshToken).isEqualTo("refresh-token")
        assertThat(result.onboardingCompleted).isFalse()
    }

    @Test
    fun `register - 자동 생성된 username은 user_ 접두사와 8자 랜덤 문자열`() {
        val request = RegisterRequest(
            email = "test@email.com",
            password = "Test1234!",
            termsAgreed = true,
            privacyAgreed = true,
            marketingAgreed = false,
        )
        val userCaptor = argumentCaptor<User>()

        given(userRepository.existsByEmail(request.email)).willReturn(false)
        given(userRepository.save(userCaptor.capture())).willAnswer { userCaptor.firstValue }
        given(jwtTokenProvider.createAccessToken(any(), any(), any())).willReturn("access-token")
        given(jwtTokenProvider.createRefreshToken()).willReturn("refresh-token")
        given(refreshTokenRepository.save(any<RefreshToken>())).willReturn(
            RefreshToken(userId = 0L, token = "refresh-token", expiresAt = LocalDateTime.now().plusDays(7))
        )

        authService.register(request)

        val capturedUsername = userCaptor.firstValue.username
        assertThat(capturedUsername).startsWith("user_")
        assertThat(capturedUsername).hasSize(13)
    }

    @Test
    fun `register - 이메일 중복 시 DUPLICATE_EMAIL 예외`() {
        val request = RegisterRequest("dup@email.com", "Test1234!", true, true, false)
        given(userRepository.existsByEmail(request.email)).willReturn(true)

        assertThatThrownBy { authService.register(request) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_EMAIL)
    }

    @Test
    fun `register - 필수 약관 미동의 시 TERMS_NOT_AGREED 예외`() {
        val request = RegisterRequest("new@email.com", "Test1234!", false, true, false)

        assertThatThrownBy { authService.register(request) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.TERMS_NOT_AGREED)
    }

    @Test
    fun `login - 정상 로그인 시 토큰 반환`() {
        val encodedPw = passwordEncoder.encode("Test1234!")
        val user = User(id = 1L, email = "test@email.com", username = "testuser", password = encodedPw,
            termsAgreed = true, privacyAgreed = true)
        val request = LoginRequest("test@email.com", "Test1234!")

        given(userRepository.findByEmail(request.email)).willReturn(Optional.of(user))
        given(jwtTokenProvider.createAccessToken(any(), any(), any())).willReturn("access-token")
        given(jwtTokenProvider.createRefreshToken()).willReturn("refresh-token")
        given(refreshTokenRepository.save(any<RefreshToken>())).willReturn(
            RefreshToken(userId = 1L, token = "refresh-token", expiresAt = LocalDateTime.now().plusDays(7))
        )

        val result = authService.login(request)

        assertThat(result.accessToken).isEqualTo("access-token")
    }

    @Test
    fun `login - 존재하지 않는 이메일 INVALID_CREDENTIALS 예외`() {
        given(userRepository.findByEmail(any())).willReturn(Optional.empty())

        assertThatThrownBy { authService.login(LoginRequest("none@email.com", "Test1234!")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS)
    }

    @Test
    fun `login - 잘못된 비밀번호 INVALID_CREDENTIALS 예외`() {
        val user = User(id = 1L, email = "test@email.com", username = "testuser",
            password = passwordEncoder.encode("CorrectPw1!"), termsAgreed = true, privacyAgreed = true)
        given(userRepository.findByEmail(any())).willReturn(Optional.of(user))

        assertThatThrownBy { authService.login(LoginRequest("test@email.com", "WrongPw1!")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS)
    }

    @Test
    fun `requestPasswordReset - 이메일 존재하면 토큰 저장 후 이벤트 발행`() {
        val user = User(id = 1L, email = "test@email.com", username = "testuser",
            password = "encoded", termsAgreed = true, privacyAgreed = true)
        given(userRepository.findByEmail("test@email.com")).willReturn(Optional.of(user))

        authService.requestPasswordReset("test@email.com")

        verify(passwordResetTokenStore).save(any<String>(), any<String>())
        val eventCaptor = argumentCaptor<PasswordResetRequestedEvent>()
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture())
        assertThat(eventCaptor.firstValue.email).isEqualTo("test@email.com")
        assertThat(eventCaptor.firstValue.token).isNotBlank()
    }

    @Test
    fun `requestPasswordReset - 이메일 존재하지 않으면 아무것도 하지 않음`() {
        given(userRepository.findByEmail("none@email.com")).willReturn(Optional.empty())

        authService.requestPasswordReset("none@email.com")

        verify(passwordResetTokenStore, never()).save(any<String>(), any<String>())
        verify(applicationEventPublisher, never()).publishEvent(any<Any>())
    }

    @Test
    fun `resetPassword - 유효한 토큰으로 비밀번호 변경`() {
        val user = User(id = 1L, email = "test@email.com", username = "testuser",
            password = passwordEncoder.encode("OldPw1!"), termsAgreed = true, privacyAgreed = true)
        given(passwordResetTokenStore.getEmail("valid-token")).willReturn("test@email.com")
        given(userRepository.findByEmail("test@email.com")).willReturn(Optional.of(user))

        authService.resetPassword("valid-token", "NewPw1!")

        assertThat(passwordEncoder.matches("NewPw1!", user.password)).isTrue()
        verify(passwordResetTokenStore).delete("valid-token")
    }

    @Test
    fun `resetPassword - 만료된 토큰이면 PASSWORD_RESET_TOKEN_EXPIRED 예외`() {
        given(passwordResetTokenStore.getEmail("expired-token")).willReturn(null)

        assertThatThrownBy { authService.resetPassword("expired-token", "NewPw1!") }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED)
    }
}
