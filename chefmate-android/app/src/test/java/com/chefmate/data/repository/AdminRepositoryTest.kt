package com.chefmate.data.repository

import com.chefmate.data.api.ApiClient
import com.chefmate.data.api.ApiService
import com.chefmate.data.api.models.UserManagementResponse
import com.chefmate.utils.TokenManager
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
class AdminRepositoryTest {

    @Mock
    private lateinit var apiService: ApiService

    @Mock
    private lateinit var tokenManager: TokenManager

    private lateinit var adminRepository: AdminRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(tokenManager.getToken()).thenReturn("Bearer test_token")
        adminRepository = AdminRepository(tokenManager, apiService)
    }

    @Test
    fun `getAllUsers should return list of users`() = runTest {
        val users = listOf(
            UserManagementResponse(1L, "user1", "user1@test.com", "USER", true),
            UserManagementResponse(2L, "user2", "user2@test.com", "ADMIN", true)
        )
        whenever(apiService.getAllUsers(any())).thenReturn(Response.success(users))

        val result = adminRepository.getAllUsers()
        assert(result.isSuccess)
        assert(result.getOrNull()?.size == 2)
    }

    @Test
    fun `blockUser should return success message`() = runTest {
        whenever(apiService.blockUser(any(), any())).thenReturn(
            Response.success(mapOf("message" to "User blocked successfully"))
        )

        val result = adminRepository.blockUser(1L)
        assert(result.isSuccess)
    }

    @Test
    fun `deleteUser should return success message`() = runTest {
        whenever(apiService.deleteUser(any(), any())).thenReturn(
            Response.success(mapOf("message" to "User deleted successfully"))
        )

        val result = adminRepository.deleteUser(1L)
        assert(result.isSuccess)
    }
}
