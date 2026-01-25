package com.chefmate.data.repository

import com.chefmate.data.api.ApiClient
import com.chefmate.data.api.ApiService
import com.chefmate.data.api.models.ShoppingListResponse
import com.chefmate.data.api.models.ShoppingListItem
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ShoppingRepositoryTest {

    @Mock
    private lateinit var apiService: ApiService

    @Mock
    private lateinit var tokenManager: TokenManager

    private lateinit var shoppingRepository: ShoppingRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        TestHelper.mockApiService(apiService)
        shoppingRepository = ShoppingRepository(apiService, tokenManager)
    }

    @Test
    fun `getMyShoppingList should return shopping list on success`() = runTest {
        val mockShoppingList = ShoppingListResponse(
            id = 1L,
            createdAt = "2024-01-01T00:00:00",
            items = listOf(
                ShoppingListItem(id = 1L, name = "Item 1", quantity = "1", unit = "kg")
            )
        )
        whenever(tokenManager.getToken()).thenReturn("test_token")
        whenever(apiService.getMyShoppingList(any())).thenReturn(Response.success(mockShoppingList))

        val result = shoppingRepository.getMyShoppingList()
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals(1, result.getOrNull()!!.items.size)
    }

    @Test
    fun `getMyShoppingList should return failure when not authenticated`() = runTest {
        whenever(tokenManager.getToken()).thenReturn(null)

        val result = shoppingRepository.getMyShoppingList()
        assertTrue(result.isFailure)
    }

    @Test
    fun `createShoppingListFromRecipe should return shopping list on success`() = runTest {
        val mockShoppingList = ShoppingListResponse(id = 1L, createdAt = "2024-01-01T00:00:00", items = emptyList())
        whenever(tokenManager.getToken()).thenReturn("test_token")
        whenever(apiService.createShoppingListFromRecipe(any(), any())).thenReturn(Response.success(mockShoppingList))

        val result = shoppingRepository.createShoppingListFromRecipe(1L)
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `updateShoppingListItem should return success`() = runTest {
        val mockItem = ShoppingListItem(id = 1L, name = "Item 1", purchased = true)
        whenever(tokenManager.getToken()).thenReturn("test_token")
        whenever(apiService.updateShoppingListItem(any(), any(), any(), any())).thenReturn(Response.success(mockItem))

        val result = shoppingRepository.updateShoppingListItem(1L, 1L, true)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `addShoppingListItem should return success`() = runTest {
        val mockItem = ShoppingListItem(id = 1L, name = "New Item")
        whenever(tokenManager.getToken()).thenReturn("test_token")
        whenever(apiService.addShoppingListItem(any(), any(), any())).thenReturn(Response.success(mockItem))

        val result = shoppingRepository.addShoppingListItem(1L, "New Item", "1", "kg")
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `deleteShoppingListItem should return success`() = runTest {
        whenever(tokenManager.getToken()).thenReturn("test_token")
        whenever(apiService.deleteShoppingListItem(any(), any(), any())).thenReturn(Response.success(Unit))

        val result = shoppingRepository.deleteShoppingListItem(1L, 1L)
        assertTrue(result.isSuccess)
    }
}
