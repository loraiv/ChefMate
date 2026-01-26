package com.chefmate.data.repository

import com.chefmate.data.api.ApiClient
import com.chefmate.data.api.ApiService
import com.chefmate.data.api.models.AuthResponse
import com.chefmate.data.api.models.LoginRequest
import com.chefmate.data.api.models.RegisterRequest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
class AuthRepositoryTest {

    @Mock
    private lateinit var apiService: ApiService

    private lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        authRepository = AuthRepository(apiService)
    }

    @Test
    fun `login with valid credentials should return success`() = runTest {
        val authResponse = AuthResponse(
            token = "test_token",
            userId = 1L,
            username = "testuser",
            email = "test@example.com"
        )
        whenever(apiService.login(any())).thenReturn(Response.success(authResponse))

        val result = authRepository.login("test@example.com", "password")
        assert(result.isSuccess)
        assert(result.getOrNull()?.token == "test_token")
    }

    @Test
    fun `login with empty credentials should return failure`() = runTest {
        val result = authRepository.login("", "")
        assert(result.isFailure)
    }

    @Test
    fun `register with valid data should return success`() = runTest {
        val authResponse = AuthResponse(
            token = "test_token",
            userId = 1L,
            username = "newuser",
            email = "new@example.com"
        )
        whenever(apiService.register(any())).thenReturn(Response.success(authResponse))

        val result = authRepository.register("newuser", "new@example.com", "password")
        assert(result.isSuccess)
    }

    @Test
    fun `register with empty data should return failure`() = runTest {
        val result = authRepository.register("", "", "")
        assert(result.isFailure)
    }
}
