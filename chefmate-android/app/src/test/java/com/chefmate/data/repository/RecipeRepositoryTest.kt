package com.chefmate.data.repository

import com.chefmate.data.api.ApiClient
import com.chefmate.data.api.ApiService
import com.chefmate.data.api.models.*
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

class RecipeRepositoryTest {

    @Mock
    private lateinit var apiService: ApiService

    @Mock
    private lateinit var tokenManager: TokenManager

    private lateinit var recipeRepository: RecipeRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        TestHelper.mockApiService(apiService)
        recipeRepository = RecipeRepository(apiService, tokenManager)
    }

    @Test
    fun `getRecipes should return list of recipes on success`() = runTest {
        val mockRecipes = listOf(
            RecipeResponse(
                id = 1L, 
                title = "Recipe 1", 
                userId = 1L, 
                username = "user1",
                difficulty = "EASY",
                imageUrl = null,
                ingredients = emptyList(),
                steps = emptyList(),
                createdAt = "2024-01-01T00:00:00",
                updatedAt = "2024-01-01T00:00:00"
            )
        )
        whenever(tokenManager.getToken()).thenReturn("test_token")
        whenever(apiService.getRecipes(any())).thenReturn(Response.success(mockRecipes))

        val result = recipeRepository.getRecipes()
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals(1, result.getOrNull()!!.size)
    }

    @Test
    fun `getRecipes should return failure when not authenticated`() = runTest {
        whenever(tokenManager.getToken()).thenReturn(null)

        val result = recipeRepository.getRecipes()
        assertTrue(result.isFailure)
    }

    @Test
    fun `getRecipeById should return recipe on success`() = runTest {
        val mockRecipe = RecipeResponse(
            id = 1L, 
            title = "Recipe 1", 
            userId = 1L, 
            username = "user1",
            difficulty = "EASY",
            imageUrl = null,
            ingredients = emptyList(),
            steps = emptyList(),
            createdAt = "2024-01-01T00:00:00",
            updatedAt = "2024-01-01T00:00:00"
        )
        whenever(tokenManager.getToken()).thenReturn("test_token")
        whenever(apiService.getRecipe(any(), any())).thenReturn(Response.success(mockRecipe))

        val result = recipeRepository.getRecipeById(1L)
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals("Recipe 1", result.getOrNull()!!.title)
    }

    @Test
    fun `likeRecipe should return success`() = runTest {
        whenever(tokenManager.getToken()).thenReturn("test_token")
        whenever(apiService.likeRecipe(any(), any())).thenReturn(Response.success(Unit))

        val result = recipeRepository.likeRecipe(1L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `unlikeRecipe should return success`() = runTest {
        whenever(tokenManager.getToken()).thenReturn("test_token")
        whenever(apiService.unlikeRecipe(any(), any())).thenReturn(Response.success(Unit))

        val result = recipeRepository.unlikeRecipe(1L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `searchRecipes should return filtered recipes`() = runTest {
        val mockRecipes = listOf(
            RecipeResponse(
                id = 1L, 
                title = "Pasta", 
                userId = 1L, 
                username = "user1",
                difficulty = "EASY",
                imageUrl = null,
                ingredients = emptyList(),
                steps = emptyList(),
                createdAt = "2024-01-01T00:00:00",
                updatedAt = "2024-01-01T00:00:00"
            )
        )
        whenever(tokenManager.getToken()).thenReturn("test_token")
        whenever(apiService.searchRecipes(any(), any(), any(), any())).thenReturn(Response.success(mockRecipes))

        val result = recipeRepository.searchRecipes("pasta", null, null)
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `deleteRecipe should return success`() = runTest {
        whenever(tokenManager.getToken()).thenReturn("test_token")
        whenever(apiService.deleteRecipe(any(), any())).thenReturn(Response.success(Unit))

        val result = recipeRepository.deleteRecipe(1L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `deleteComment should return success`() = runTest {
        whenever(tokenManager.getToken()).thenReturn("test_token")
        whenever(apiService.deleteComment(any(), any())).thenReturn(Response.success(Unit))

        val result = recipeRepository.deleteComment(1L)
        assertTrue(result.isSuccess)
    }
}
