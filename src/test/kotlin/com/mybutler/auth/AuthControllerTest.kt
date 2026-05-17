package com.mybutler.auth

import com.mybutler.auth.controller.AuthController
import com.mybutler.auth.dto.AuthTokens
import com.mybutler.auth.dto.LoginRequest
import com.mybutler.auth.dto.RefreshTokenRequest
import com.mybutler.auth.dto.RegisterRequest
import com.mybutler.auth.service.AuthService
import com.mybutler.common.security.CustomUserDetails
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import org.mockito.kotlin.verify

@ExtendWith(MockitoExtension::class)
class AuthControllerTest {

    @Mock lateinit var authService: AuthService

    private lateinit var authController: AuthController

    @BeforeEach
    fun setUp() {
        authController = AuthController(authService)
    }

    @Test
    fun `register - refresh token을 response body에 포함한다`() {
        val request = RegisterRequest(
            email = "user@example.com",
            password = "Passw0rd!",
            termsAgreed = true,
            privacyAgreed = true,
            marketingAgreed = false,
        )
        given(authService.register(request)).willReturn(
            AuthTokens(
                accessToken = "access-token",
                refreshToken = "refresh-token",
                onboardingCompleted = false,
            )
        )

        val response = authController.register(request)

        assertThat(response.data).isNotNull
        assertThat(response.data!!.accessToken).isEqualTo("access-token")
        assertThat(response.data!!.refreshToken).isEqualTo("refresh-token")
        assertThat(response.data!!.onboardingCompleted).isFalse()
    }

    @Test
    fun `login - refresh token을 response body에 포함한다`() {
        val request = LoginRequest(
            email = "user@example.com",
            password = "Passw0rd!",
        )
        given(authService.login(request)).willReturn(
            AuthTokens(
                accessToken = "access-token",
                refreshToken = "refresh-token",
                onboardingCompleted = true,
            )
        )

        val response = authController.login(request)

        assertThat(response.data).isNotNull
        assertThat(response.data!!.refreshToken).isEqualTo("refresh-token")
        assertThat(response.data!!.onboardingCompleted).isTrue()
    }

    @Test
    fun `refresh - request body의 refresh token으로 재발급한다`() {
        val request = RefreshTokenRequest(refreshToken = "refresh-token")
        given(authService.refresh("refresh-token")).willReturn(
            AuthTokens(
                accessToken = "new-access-token",
                refreshToken = "new-refresh-token",
                onboardingCompleted = true,
            )
        )

        val response = authController.refresh(request)

        verify(authService).refresh("refresh-token")
        assertThat(response.data).isNotNull
        assertThat(response.data!!.accessToken).isEqualTo("new-access-token")
        assertThat(response.data!!.refreshToken).isEqualTo("new-refresh-token")
    }

    @Test
    fun `logout - 사용자 refresh token을 서버에서 제거한다`() {
        val userDetails = CustomUserDetails(
            userId = 1L,
            email = "user@example.com",
            username = "tester",
        )

        val response = authController.logout(userDetails)

        verify(authService).logout(1L)
        assertThat(response.success).isTrue()
        assertThat(response.data).isNull()
    }
}
