package com.chefmate.data.repository

import com.chefmate.data.api.ApiClient
import com.chefmate.data.api.ApiService
import com.chefmate.data.api.models.AiRequest
import com.chefmate.data.api.models.AiResponse
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class AiRepository(
    private val tokenManager: TokenManager,
    private val apiService: ApiService = ApiClient.apiService
) {

    private fun getAuthToken(): String {
        val token = tokenManager.getToken()
        return if (token.isNullOrEmpty()) {
            ""
        } else {
            if (token.startsWith("Bearer ")) token else "Bearer $token"
        }
    }

    suspend fun chatWithAI(message: String, recipeId: Long? = null, cookingContext: com.chefmate.data.api.models.CookingContext? = null): Result<AiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAuthToken()
                if (token.isBlank()) {
                    return@withContext Result.failure(Exception("You are not authenticated"))
                }

                val request = AiRequest(message = message, recipeId = recipeId, cookingContext = cookingContext)
                val response = apiService.chatWithAI(token, request)

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception("Error communicating with AI: $errorBody"))
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: e.message()
                Result.failure(Exception("HTTP error: $errorBody"))
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }
}

