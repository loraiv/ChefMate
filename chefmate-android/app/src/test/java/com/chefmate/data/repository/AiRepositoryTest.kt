package com.chefmate.data.repository

import com.chefmate.data.api.ApiClient
import com.chefmate.data.api.ApiService
import com.chefmate.data.api.models.AiRequest
import com.chefmate.data.api.models.AiResponse
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AiRepositoryTest {

    @Mock
    private lateinit var apiService: ApiService

    @Mock
    private lateinit var tokenManager: TokenManager

    private lateinit var aiRepository: AiRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        aiRepository = AiRepository(tokenManager, apiService)
    }

    @Test
    fun `chatWithAI should return response on success`() = runTest {
        val mockResponse = AiResponse(response = "AI response")
        whenever(tokenManager.getToken()).thenReturn("test_token")
        whenever(apiService.chatWithAI(any(), any())).thenReturn(Response.success(mockResponse))

        val result = aiRepository.chatWithAI("Hello")
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals("AI response", result.getOrNull()!!.response)
    }

    @Test
    fun `chatWithAI should return failure when not authenticated`() = runTest {
        whenever(tokenManager.getToken()).thenReturn(null)

        val result = aiRepository.chatWithAI("Hello")
        assertTrue(result.isFailure)
    }

    @Test
    fun `chatWithAI should handle HTTP errors`() = runTest {
        whenever(tokenManager.getToken()).thenReturn("test_token")
        whenever(apiService.chatWithAI(any(), any())).thenAnswer {
            throw HttpException(Response.error<Any>(500, okhttp3.ResponseBody.create(null, "")))
        }

        val result = aiRepository.chatWithAI("Hello")
        assertTrue(result.isFailure)
    }

    @Test
    fun `chatWithAI should handle network errors`() = runTest {
        whenever(tokenManager.getToken()).thenReturn("test_token")
        whenever(apiService.chatWithAI(any(), any())).thenAnswer {
            throw java.io.IOException("Network error")
        }

        val result = aiRepository.chatWithAI("Hello")
        assertTrue(result.isFailure)
        val errorMessage = result.exceptionOrNull()?.message ?: ""
        assertTrue(errorMessage.contains("Network error", ignoreCase = true))
    }

    @Test
    fun `chatWithAI should handle HttpException`() = runTest {
        whenever(tokenManager.getToken()).thenReturn("test_token")
        whenever(apiService.chatWithAI(any(), any())).thenAnswer {
            throw HttpException(Response.error<Any>(429, okhttp3.ResponseBody.create(null, "")))
        }

        val result = aiRepository.chatWithAI("Hello")
        assertTrue(result.isFailure)
    }
}
