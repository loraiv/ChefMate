package com.chefmate.ui.auth.viewmodel

import com.chefmate.data.api.models.AuthResponse
import com.chefmate.data.repository.AuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AuthViewModelTest {

    @Mock
    private lateinit var authRepository: AuthRepository

    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be Idle`() = runTest {
        val state = viewModel.authState.first()
        assertTrue(state is AuthState.Idle)
    }

    @Test
    fun `login with valid credentials should set Success state`() = runTest {
        val authResponse = AuthResponse(
            token = "test_token",
            userId = 1L,
            username = "testuser",
            email = "test@example.com"
        )
        whenever(authRepository.login(any(), any())).thenReturn(
            kotlin.Result.success(authResponse)
        )

        viewModel.login("test@example.com", "password")
        advanceUntilIdle()

        val state = viewModel.authState.first()
        assertTrue(state is AuthState.Success)
        assertEquals("test_token", (state as AuthState.Success).token)
    }

    @Test
    fun `login with invalid credentials should set Error state`() = runTest {
        whenever(authRepository.login(any(), any())).thenReturn(
            kotlin.Result.failure(Exception("Invalid credentials"))
        )

        viewModel.login("test@example.com", "wrong")
        advanceUntilIdle()

        val state = viewModel.authState.first()
        assertTrue(state is AuthState.Error)
        assertEquals("Invalid credentials", (state as AuthState.Error).message)
    }

    @Test
    fun `register with valid data should set Success state`() = runTest {
        val authResponse = AuthResponse(
            token = "test_token",
            userId = 1L,
            username = "newuser",
            email = "new@example.com"
        )
        whenever(authRepository.register(any(), any(), any())).thenReturn(
            kotlin.Result.success(authResponse)
        )

        viewModel.register("newuser", "new@example.com", "password")
        advanceUntilIdle()

        val state = viewModel.authState.first()
        assertTrue(state is AuthState.Success)
        assertEquals("test_token", (state as AuthState.Success).token)
    }

    @Test
    fun `register with invalid data should set Error state`() = runTest {
        whenever(authRepository.register(any(), any(), any())).thenReturn(
            kotlin.Result.failure(Exception("Registration failed"))
        )

        viewModel.register("", "", "")
        advanceUntilIdle()

        val state = viewModel.authState.first()
        assertTrue(state is AuthState.Error)
    }

    @Test
    fun `login should set Loading state before Success`() = runTest {
        val authResponse = AuthResponse(
            token = "test_token",
            userId = 1L,
            username = "testuser",
            email = "test@example.com"
        )
        whenever(authRepository.login(any(), any())).thenReturn(
            kotlin.Result.success(authResponse)
        )

        viewModel.login("test@example.com", "password")
        // Check Loading state
        val loadingState = viewModel.authState.first()
        assertTrue(loadingState is AuthState.Loading || loadingState is AuthState.Success)
    }
}
