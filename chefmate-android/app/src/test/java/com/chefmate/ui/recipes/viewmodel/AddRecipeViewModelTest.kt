package com.chefmate.ui.recipes.viewmodel

import com.chefmate.data.api.models.RecipeResponse
import com.chefmate.data.repository.RecipeRepository
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
class AddRecipeViewModelTest {

    @Mock
    private lateinit var recipeRepository: RecipeRepository

    private lateinit var viewModel: AddRecipeViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        viewModel = AddRecipeViewModel(recipeRepository)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `createRecipe should set recipeCreated on success`() = runTest {
        val mockRecipe = RecipeResponse(
            id = 1L,
            title = "New Recipe",
            userId = 1L,
            username = "user1",
            difficulty = "EASY",
            imageUrl = null,
            ingredients = emptyList(),
            steps = emptyList(),
            createdAt = "2024-01-01T00:00:00",
            updatedAt = "2024-01-01T00:00:00"
        )
        whenever(recipeRepository.createRecipe(any(), any())).thenReturn(
            kotlin.Result.success(mockRecipe)
        )

        viewModel.createRecipe(
            title = "New Recipe",
            description = "Description",
            difficulty = "EASY",
            prepTime = 10,
            cookTime = 20,
            servings = 4,
            ingredients = listOf("Ingredient 1"),
            steps = listOf("Step 1"),
            imagePaths = null
        )
        advanceUntilIdle()

        val recipeCreated = viewModel.recipeCreated.first()
        assertTrue(recipeCreated != null)
        assertEquals("New Recipe", recipeCreated!!.title)
    }

    @Test
    fun `createRecipe should set error on failure`() = runTest {
        whenever(recipeRepository.createRecipe(any(), any())).thenReturn(
            kotlin.Result.failure(Exception("Creation failed"))
        )

        viewModel.createRecipe(
            title = "New Recipe",
            description = null,
            difficulty = "EASY",
            prepTime = null,
            cookTime = null,
            servings = null,
            ingredients = emptyList(),
            steps = emptyList(),
            imagePaths = null
        )
        advanceUntilIdle()

        val error = viewModel.error.first()
        assertTrue(error != null)
        assertTrue(error!!.contains("Creation failed"))
    }

    @Test
    fun `updateRecipe should set recipeCreated on success`() = runTest {
        val mockRecipe = RecipeResponse(
            id = 1L,
            title = "Updated Recipe",
            userId = 1L,
            username = "user1",
            difficulty = "EASY",
            imageUrl = null,
            ingredients = emptyList(),
            steps = emptyList(),
            createdAt = "2024-01-01T00:00:00",
            updatedAt = "2024-01-01T00:00:00"
        )
        whenever(recipeRepository.updateRecipe(any(), any(), any(), any())).thenReturn(
            kotlin.Result.success(mockRecipe)
        )

        viewModel.updateRecipe(
            recipeId = 1L,
            title = "Updated Recipe",
            description = "Updated description",
            difficulty = "MEDIUM",
            prepTime = 15,
            cookTime = 25,
            servings = 6,
            ingredients = listOf("Updated ingredient"),
            steps = listOf("Updated step"),
            imagePaths = null,
            existingImageUrls = emptyList()
        )
        advanceUntilIdle()

        val recipeCreated = viewModel.recipeCreated.first()
        assertTrue(recipeCreated != null)
        assertEquals("Updated Recipe", recipeCreated!!.title)
    }

    @Test
    fun `updateRecipe should set error on failure`() = runTest {
        whenever(recipeRepository.updateRecipe(any(), any(), any(), any())).thenReturn(
            kotlin.Result.failure(Exception("Update failed"))
        )

        viewModel.updateRecipe(
            recipeId = 1L,
            title = "Recipe",
            description = null,
            difficulty = "EASY",
            prepTime = null,
            cookTime = null,
            servings = null,
            ingredients = emptyList(),
            steps = emptyList(),
            imagePaths = null,
            existingImageUrls = emptyList()
        )
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
    fun `clearRecipeCreated should clear recipe created state`() = runTest {
        viewModel.clearRecipeCreated()
        advanceUntilIdle()

        val recipeCreated = viewModel.recipeCreated.first()
        assertTrue(recipeCreated == null)
    }
}
