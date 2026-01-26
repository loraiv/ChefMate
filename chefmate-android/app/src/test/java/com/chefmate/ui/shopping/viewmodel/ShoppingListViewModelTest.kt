package com.chefmate.ui.shopping.viewmodel

import com.chefmate.data.api.models.ShoppingListResponse
import com.chefmate.data.api.models.ShoppingListItem
import com.chefmate.data.repository.ShoppingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class ShoppingListViewModelTest {

    @Mock
    private lateinit var shoppingRepository: ShoppingRepository

    private lateinit var viewModel: ShoppingListViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        
        // Mock shopping list flow
        val shoppingListFlow = MutableStateFlow<ShoppingListResponse?>(null)
        whenever(shoppingRepository.shoppingList).thenReturn(shoppingListFlow)
        
        viewModel = ShoppingListViewModel(shoppingRepository)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `loadShoppingList should update shopping list on success`() = runTest {
        val mockShoppingList = ShoppingListResponse(
            id = 1L,
            createdAt = "2024-01-01T00:00:00",
            items = listOf(
                ShoppingListItem(id = 1L, name = "Item 1", quantity = "1", unit = "kg", purchased = false)
            )
        )
        whenever(shoppingRepository.getMyShoppingList()).thenReturn(
            kotlin.Result.success(mockShoppingList)
        )
        
        // Update the flow to simulate repository updating it
        (shoppingRepository.shoppingList as MutableStateFlow).value = mockShoppingList

        viewModel.loadShoppingList()
        advanceUntilIdle()

        val shoppingList = viewModel.shoppingList.first()
        assertTrue(shoppingList != null)
        assertEquals(1, shoppingList!!.items.size)
    }

    @Test
    fun `loadShoppingList should set error on failure`() = runTest {
        whenever(shoppingRepository.getMyShoppingList()).thenReturn(
            kotlin.Result.failure(Exception("Network error"))
        )

        viewModel.loadShoppingList()
        advanceUntilIdle()

        val error = viewModel.error.first()
        assertTrue(error != null)
    }

    @Test
    fun `updateItem should update item checked status`() = runTest {
        val item = ShoppingListItem(id = 1L, name = "Item 1", purchased = false)
        val shoppingList = ShoppingListResponse(id = 1L, createdAt = "2024-01-01T00:00:00", items = listOf(item))
        
        // Set shopping list in flow
        (shoppingRepository.shoppingList as MutableStateFlow).value = shoppingList
        
        val updatedItem = item.copy(purchased = true)
        whenever(shoppingRepository.updateShoppingListItem(any(), any(), any())).thenReturn(
            kotlin.Result.success(updatedItem)
        )

        viewModel.updateItem(item, true)
        advanceUntilIdle()

        val message = viewModel.message.first()
        assertTrue(message != null)
        assertTrue(message!!.contains("purchased"))
    }

    @Test
    fun `addItem should add new item to list`() = runTest {
        val shoppingList = ShoppingListResponse(id = 1L, createdAt = "2024-01-01T00:00:00", items = emptyList())
        (shoppingRepository.shoppingList as MutableStateFlow).value = shoppingList
        
        val newItem = ShoppingListItem(id = 2L, name = "New Item", quantity = "2", unit = "kg")
        whenever(shoppingRepository.addShoppingListItem(any(), any(), any(), any())).thenReturn(
            kotlin.Result.success(newItem)
        )
        whenever(shoppingRepository.getMyShoppingList()).thenReturn(
            kotlin.Result.success(shoppingList)
        )

        viewModel.addItem("New Item", "2", "kg")
        advanceUntilIdle()

        val message = viewModel.message.first()
        assertTrue(message != null)
        assertTrue(message!!.contains("added"))
    }

    @Test
    fun `deleteItem should remove item from list`() = runTest {
        val item = ShoppingListItem(id = 1L, name = "Item 1")
        val shoppingList = ShoppingListResponse(id = 1L, createdAt = "2024-01-01T00:00:00", items = listOf(item))
        (shoppingRepository.shoppingList as MutableStateFlow).value = shoppingList
        
        whenever(shoppingRepository.deleteShoppingListItem(any(), any())).thenReturn(
            kotlin.Result.success(true)
        )
        whenever(shoppingRepository.getMyShoppingList()).thenReturn(
            kotlin.Result.success(ShoppingListResponse(id = 1L, createdAt = "2024-01-01T00:00:00", items = emptyList()))
        )

        viewModel.deleteItem(item)
        advanceUntilIdle()

        val message = viewModel.message.first()
        assertTrue(message != null)
        assertTrue(message!!.contains("removed"))
    }

    @Test
    fun `clearError should clear error state`() = runTest {
        viewModel.clearError()
        advanceUntilIdle()

        val error = viewModel.error.first()
        assertTrue(error == null)
    }

    @Test
    fun `clearMessage should clear message state`() = runTest {
        viewModel.clearMessage()
        advanceUntilIdle()

        val message = viewModel.message.first()
        assertTrue(message == null)
    }
}
