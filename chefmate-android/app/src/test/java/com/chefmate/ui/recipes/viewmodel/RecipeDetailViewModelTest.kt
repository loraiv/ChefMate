package com.chefmate.ui.recipes.viewmodel

import com.chefmate.data.api.models.RecipeResponse
import com.chefmate.data.repository.RecipeRepository
import com.chefmate.data.repository.ShoppingRepository
import com.chefmate.data.repository.AdminRepository
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
class RecipeDetailViewModelTest {

    @Mock
    private lateinit var recipeRepository: RecipeRepository

    @Mock
    private lateinit var shoppingRepository: ShoppingRepository

    @Mock
    private lateinit var adminRepository: AdminRepository

    private lateinit var viewModel: RecipeDetailViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        viewModel = RecipeDetailViewModel(recipeRepository, shoppingRepository, adminRepository)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `loadRecipe should update recipe on success`() = runTest {
        val mockRecipe = RecipeResponse(
            id = 1L,
            title = "Test Recipe",
            userId = 1L,
            username = "user1",
            difficulty = "EASY",
            imageUrl = null,
            ingredients = emptyList(),
            steps = emptyList(),
            createdAt = "2024-01-01T00:00:00",
            updatedAt = "2024-01-01T00:00:00"
        )
        whenever(recipeRepository.getRecipeById(any())).thenReturn(
            kotlin.Result.success(mockRecipe)
        )

        viewModel.loadRecipe(1L)
        advanceUntilIdle()

        val recipe = viewModel.recipe.first()
        assertTrue(recipe != null)
        assertEquals("Test Recipe", recipe!!.title)
    }

    @Test
    fun `loadRecipe should set error on failure`() = runTest {
        whenever(recipeRepository.getRecipeById(any())).thenReturn(
            kotlin.Result.failure(Exception("Recipe not found"))
        )

        viewModel.loadRecipe(1L)
        advanceUntilIdle()

        val error = viewModel.error.first()
        assertTrue(error != null)
    }

    @Test
    fun `toggleLike should update recipe like status`() = runTest {
        val recipe = RecipeResponse(
            id = 1L,
            title = "Recipe",
            userId = 1L,
            username = "user1",
            isLiked = false,
            likesCount = 5,
            difficulty = "EASY",
            imageUrl = null,
            ingredients = emptyList(),
            steps = emptyList(),
            createdAt = "2024-01-01T00:00:00",
            updatedAt = "2024-01-01T00:00:00"
        )
        whenever(recipeRepository.getRecipeById(any())).thenReturn(
            kotlin.Result.success(recipe)
        )
        whenever(recipeRepository.likeRecipe(any())).thenReturn(
            kotlin.Result.success(true)
        )

        viewModel.loadRecipe(1L)
        advanceUntilIdle()

        val loadedRecipe = viewModel.recipe.first()!!
        viewModel.toggleLike(loadedRecipe)
        advanceUntilIdle()

        val updatedRecipe = viewModel.recipe.first()
        assertTrue(updatedRecipe!!.isLiked)
        assertEquals(6, updatedRecipe.likesCount)
    }

    @Test
    fun `addToShoppingList should set success message`() = runTest {
        whenever(shoppingRepository.createShoppingListFromRecipe(any())).thenReturn(
            kotlin.Result.success(
                com.chefmate.data.api.models.ShoppingListResponse(
                    id = 1L,
                    createdAt = "2024-01-01T00:00:00",
                    items = emptyList()
                )
            )
        )
        whenever(shoppingRepository.getMyShoppingList()).thenReturn(
            kotlin.Result.success(
                com.chefmate.data.api.models.ShoppingListResponse(
                    id = 1L,
                    createdAt = "2024-01-01T00:00:00",
                    items = emptyList()
                )
            )
        )

        viewModel.addToShoppingList(1L)
        advanceUntilIdle()

        val message = viewModel.shoppingListMessage.first()
        assertTrue(message != null)
        assertTrue(message!!.contains("added"))
    }

    @Test
    fun `deleteRecipe should set success message for admin`() = runTest {
        whenever(adminRepository.deleteRecipeAsAdmin(any())).thenReturn(
            kotlin.Result.success("Recipe deleted successfully")
        )

        viewModel.deleteRecipe(1L, isAdmin = true)
        advanceUntilIdle()

        val error = viewModel.error.first()
        assertTrue(error != null)
        assertTrue(error!!.contains("deleted"))
    }

    @Test
    fun `deleteRecipe should set success message for regular user`() = runTest {
        whenever(recipeRepository.deleteRecipe(any())).thenReturn(
            kotlin.Result.success(true)
        )

        viewModel.deleteRecipe(1L, isAdmin = false)
        advanceUntilIdle()

        val error = viewModel.error.first()
        assertTrue(error != null)
    }

    @Test
    fun `clearError should clear error state`() = runTest {
        viewModel.clearError()
        advanceUntilIdle()

        val error = viewModel.error.first()
        assertTrue(error == null)
    }

    @Test
    fun `clearShoppingListMessage should clear message`() = runTest {
        viewModel.clearShoppingListMessage()
        advanceUntilIdle()

        val message = viewModel.shoppingListMessage.first()
        assertTrue(message == null)
    }
}
