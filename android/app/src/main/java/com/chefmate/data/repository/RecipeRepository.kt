package com.chefmate.data.repository

import com.chefmate.data.api.ApiService
import com.chefmate.data.api.models.*
import com.chefmate.utils.TokenManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class RecipeRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    private val _recipes = MutableStateFlow<List<RecipeResponse>>(emptyList())
    val recipes: StateFlow<List<RecipeResponse>> = _recipes.asStateFlow()

    private val _currentRecipe = MutableStateFlow<RecipeResponse?>(null)
    val currentRecipe: StateFlow<RecipeResponse?> = _currentRecipe.asStateFlow()

    private fun getAuthToken(): String {
        val token = tokenManager.getToken()
        return if (token.isNullOrEmpty()) {
            ""
        } else {
            "Bearer $token"
        }
    }

    suspend fun getRecipes(): Result<List<RecipeResponse>> {
        return try {
            val token = getAuthToken()
            if (token.isBlank()) {
                return Result.failure(Exception("Not authenticated"))
            }

            val response = apiService.getRecipes(token)

            if (response.isSuccessful) {
                val recipesList = response.body() ?: emptyList()
                _recipes.value = recipesList
                Result.success(recipesList)
            } else {
                Result.failure(Exception("Failed to fetch recipes: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecipeById(id: Long): Result<RecipeResponse> {
        return try {
            val token = getAuthToken()
            val response = apiService.getRecipe(token, id)

            if (response.isSuccessful) {
                val recipe = response.body()!!
                _currentRecipe.value = recipe
                Result.success(recipe)
            } else {
                Result.failure(Exception("Failed to fetch recipe"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRecipe(recipe: RecipeRequest, imagePath: String?): Result<RecipeResponse> {
        return try {
            val token = getAuthToken()

            // Prepare image part if exists
            val imagePart = imagePath?.let { path ->
                val file = File(path)
                MultipartBody.Part.createFormData(
                    "image",
                    file.name,
                    file.asRequestBody("image/*".toMediaTypeOrNull())
                )
            }

            // Convert recipe to JSON
            val json = Gson().toJson(recipe)
            val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

            val response = apiService.createRecipe(token, requestBody, imagePart)

            if (response.isSuccessful) {
                val newRecipe = response.body()!!
                // Add to current list
                _recipes.value = _recipes.value + newRecipe
                Result.success(newRecipe)
            } else {
                Result.failure(Exception("Failed to create recipe: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRecipe(id: Long, recipe: RecipeRequest): Result<RecipeResponse> {
        return try {
            val token = getAuthToken()
            val response = apiService.updateRecipe(token, id, recipe)

            if (response.isSuccessful) {
                val updatedRecipe = response.body()!!
                // Update in current list
                _recipes.value = _recipes.value.map {
                    if (it.id == id) updatedRecipe else it
                }
                Result.success(updatedRecipe)
            } else {
                Result.failure(Exception("Failed to update recipe"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRecipe(id: Long): Result<Boolean> {
        return try {
            val token = getAuthToken()
            val response = apiService.deleteRecipe(token, id)

            if (response.isSuccessful) {
                // Remove from current list
                _recipes.value = _recipes.value.filter { it.id != id }
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to delete recipe"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchRecipes(
        query: String? = null,
        difficulty: String? = null,
        maxTime: Int? = null
    ): Result<List<RecipeResponse>> {
        return try {
            val token = getAuthToken()
            val response = apiService.searchRecipes(token, query, difficulty, maxTime)

            if (response.isSuccessful) {
                val recipes = response.body() ?: emptyList()
                _recipes.value = recipes
                Result.success(recipes)
            } else {
                Result.failure(Exception("Search failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun likeRecipe(recipeId: Long): Result<Boolean> {
        return try {
            val token = getAuthToken()
            val response = apiService.likeRecipe(token, recipeId)

            if (response.isSuccessful) {
                // Update in current list
                _recipes.value = _recipes.value.map { recipe ->
                    if (recipe.id == recipeId) {
                        recipe.copy(
                            likesCount = recipe.likesCount + 1,
                            isLiked = true
                        )
                    } else recipe
                }
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to like recipe"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlikeRecipe(recipeId: Long): Result<Boolean> {
        return try {
            val token = getAuthToken()
            val response = apiService.unlikeRecipe(token, recipeId)

            if (response.isSuccessful) {
                // Update in current list
                _recipes.value = _recipes.value.map { recipe ->
                    if (recipe.id == recipeId) {
                        recipe.copy(
                            likesCount = maxOf(0, recipe.likesCount - 1),
                            isLiked = false
                        )
                    } else recipe
                }
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to unlike recipe"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getComments(recipeId: Long): Result<List<Comment>> {
        return try {
            val token = getAuthToken()
            val response = apiService.getRecipeComments(token, recipeId)

            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch comments"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addComment(recipeId: Long, content: String): Result<Comment> {
        return try {
            val token = getAuthToken()
            val commentBody = mapOf("content" to content)
            val response = apiService.addComment(token, recipeId, commentBody)

            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to add comment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}