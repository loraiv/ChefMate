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

    suspend fun getUserRecipes(userId: Long): Result<List<RecipeResponse>> {
        return try {
            val token = getAuthToken()
            if (token.isBlank()) {
                return Result.failure(Exception("Not authenticated"))
            }
            
            val response = apiService.getUserRecipes(token, userId)

            if (response.isSuccessful) {
                val recipes = response.body() ?: emptyList()
                Result.success(recipes)
            } else {
                val errorMessage = try {
                    response.errorBody()?.string() ?: "Unknown error"
                } catch (e: Exception) {
                    "Error code: ${response.code()}"
                }
                Result.failure(Exception("Failed to fetch user recipes: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error fetching user recipes: ${e.message}", e))
        }
    }

    suspend fun createRecipe(recipe: RecipeRequest, imagePaths: List<String>?): Result<RecipeResponse> {
        return try {
            val token = getAuthToken()

            // Prepare image parts if exist
            val imageParts = imagePaths?.mapNotNull { path ->
                val file = File(path)
                if (file.exists()) {
                    MultipartBody.Part.createFormData(
                        "images",
                        file.name,
                        file.asRequestBody("image/*".toMediaTypeOrNull())
                    )
                } else {
                    null
                }
            }?.takeIf { it.isNotEmpty() }

            // Convert recipe to JSON and send as RequestBody with text/plain content type
            val json = Gson().toJson(recipe)
            val recipeRequestBody = json.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = apiService.createRecipe(token, recipeRequestBody, imageParts)

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

    suspend fun updateRecipe(
        id: Long, 
        recipe: RecipeRequest, 
        imagePaths: List<String>?,
        existingImageUrls: List<String>
    ): Result<RecipeResponse> {
        return try {
            val token = getAuthToken()

            // Prepare image parts if exist
            val imageParts = imagePaths?.mapNotNull { path ->
                val file = File(path)
                if (file.exists()) {
                    MultipartBody.Part.createFormData(
                        "images",
                        file.name,
                        file.asRequestBody("image/*".toMediaTypeOrNull())
                    )
                } else {
                    null
                }
            }?.takeIf { it.isNotEmpty() }

            // Convert recipe to JSON and send as RequestBody with text/plain content type
            val json = Gson().toJson(recipe)
            val recipeRequestBody = json.toRequestBody("text/plain".toMediaTypeOrNull())

            // Convert existing image URLs to JSON
            val existingImageUrlsJson = Gson().toJson(existingImageUrls)
            val existingImageUrlsRequestBody = existingImageUrlsJson.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = apiService.updateRecipe(token, id, recipeRequestBody, imageParts, existingImageUrlsRequestBody)

            if (response.isSuccessful && response.body() != null) {
                val updatedRecipe = response.body()!!
                // Update in current list
                _recipes.value = _recipes.value.map {
                    if (it.id == id) updatedRecipe else it
                }
                Result.success(updatedRecipe)
            } else {
                val errorBody = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception("Failed to update recipe: ${response.code()} - $errorBody"))
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
                val comments = response.body() ?: emptyList()
                android.util.Log.d("RecipeRepository", "Received ${comments.size} comments from API")
                // Log replies for debugging
                comments.forEach { comment ->
                    val repliesCount = comment.replies?.size ?: 0
                    if (repliesCount > 0) {
                        android.util.Log.d("RecipeRepository", "Comment ${comment.id} has $repliesCount replies: ${comment.replies?.map { it.id }}")
                    }
                }
                // Validate and filter out invalid comments
                val validComments = comments.filter { comment ->
                    comment.id > 0 && comment.userId > 0 && comment.userName.isNotBlank()
                }
                android.util.Log.d("RecipeRepository", "Valid comments after filtering: ${validComments.size}")
                Result.success(validComments)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("RecipeRepository", "Failed to fetch comments: ${response.code()}, $errorBody")
                Result.failure(Exception("Failed to fetch comments: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("RecipeRepository", "Error fetching comments", e)
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

    suspend fun replyToComment(commentId: Long, content: String): Result<Comment> {
        return try {
            val token = getAuthToken()
            val replyBody = mapOf("content" to content)
            val response = apiService.replyToComment(token, commentId, replyBody)

            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to reply to comment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun likeComment(commentId: Long): Result<Boolean> {
        return try {
            val token = getAuthToken()
            val response = apiService.likeComment(token, commentId)

            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to like comment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlikeComment(commentId: Long): Result<Boolean> {
        return try {
            val token = getAuthToken()
            val response = apiService.unlikeComment(token, commentId)

            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to unlike comment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteComment(commentId: Long): Result<Boolean> {
        return try {
            val token = getAuthToken()
            val response = apiService.deleteComment(token, commentId)

            if (response.isSuccessful) {
                android.util.Log.d("RecipeRepository", "Comment $commentId deleted successfully")
                Result.success(true)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("RecipeRepository", "Failed to delete comment: ${response.code()}, $errorBody")
                val errorMessage = when (response.code()) {
                    404 -> "Коментарът не е намерен"
                    403 -> "Нямате право да изтриете този коментар"
                    401 -> "Не сте авторизирани"
                    else -> "Грешка при изтриване на коментара: ${response.code()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            android.util.Log.e("RecipeRepository", "Error deleting comment", e)
            Result.failure(Exception("Грешка при изтриване: ${e.message}"))
        }
    }
}