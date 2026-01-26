package com.chefmate.data.repository

import com.chefmate.data.api.ApiClient
import com.chefmate.data.api.ApiService
import com.chefmate.data.api.models.UserManagementResponse
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class AdminRepository(
    private val tokenManager: TokenManager,
    private val apiService: ApiService = ApiClient.apiService
) {

    private fun getAuthToken(): String {
        val token = tokenManager.getToken() ?: throw IllegalStateException("No authentication token available")
        return if (token.startsWith("Bearer ")) token else "Bearer $token"
    }

    suspend fun getAllUsers(): Result<List<UserManagementResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAuthToken()
                val response = apiService.getAllUsers(token)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception("Failed to get users: $errorBody"))
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: e.message()
                Result.failure(Exception("HTTP error: $errorBody"))
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun getUserById(userId: Long): Result<UserManagementResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAuthToken()
                val response = apiService.getUserById(token, userId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception("Failed to get user: $errorBody"))
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: e.message()
                Result.failure(Exception("HTTP error: $errorBody"))
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun blockUser(userId: Long): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAuthToken()
                val response = apiService.blockUser(token, userId)
                if (response.isSuccessful && response.body() != null) {
                    val message = response.body()!!["message"] ?: "User blocked successfully"
                    Result.success(message)
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception("Failed to block user: $errorBody"))
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: e.message()
                Result.failure(Exception("HTTP error: $errorBody"))
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun unblockUser(userId: Long): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAuthToken()
                val response = apiService.unblockUser(token, userId)
                if (response.isSuccessful && response.body() != null) {
                    val message = response.body()!!["message"] ?: "User unblocked successfully"
                    Result.success(message)
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception("Failed to unblock user: $errorBody"))
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: e.message()
                Result.failure(Exception("HTTP error: $errorBody"))
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun deleteUser(userId: Long): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAuthToken()
                val response = apiService.deleteUser(token, userId)
                if (response.isSuccessful && response.body() != null) {
                    val message = response.body()!!["message"] ?: "User deleted successfully"
                    Result.success(message)
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception("Failed to delete user: $errorBody"))
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: e.message()
                Result.failure(Exception("HTTP error: $errorBody"))
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun promoteToAdmin(userId: Long): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAuthToken()
                val response = apiService.promoteToAdmin(token, userId)
                if (response.isSuccessful && response.body() != null) {
                    val message = response.body()!!["message"] ?: "User promoted to admin successfully"
                    Result.success(message)
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception("Failed to promote user: $errorBody"))
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: e.message()
                Result.failure(Exception("HTTP error: $errorBody"))
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun demoteFromAdmin(userId: Long): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAuthToken()
                val response = apiService.demoteFromAdmin(token, userId)
                if (response.isSuccessful && response.body() != null) {
                    val message = response.body()!!["message"] ?: "User demoted from admin successfully"
                    Result.success(message)
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception("Failed to demote user: $errorBody"))
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: e.message()
                Result.failure(Exception("HTTP error: $errorBody"))
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun deleteRecipeAsAdmin(recipeId: Long): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAuthToken()
                val response = apiService.deleteRecipeAsAdmin(token, recipeId)
                if (response.isSuccessful && response.body() != null) {
                    val message = response.body()!!["message"] ?: "Recipe deleted successfully"
                    Result.success(message)
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception("Failed to delete recipe: $errorBody"))
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: e.message()
                Result.failure(Exception("HTTP error: $errorBody"))
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun deleteCommentAsAdmin(commentId: Long): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAuthToken()
                val response = apiService.deleteCommentAsAdmin(token, commentId)
                if (response.isSuccessful && response.body() != null) {
                    val message = response.body()!!["message"] ?: "Comment deleted successfully"
                    Result.success(message)
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception("Failed to delete comment: $errorBody"))
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
