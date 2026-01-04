package com.chefmate.data.repository

import com.chefmate.data.api.ApiClient
import com.chefmate.data.api.models.AuthResponse
import com.chefmate.data.api.models.ChangePasswordRequest
import com.chefmate.data.api.models.ForgotPasswordRequest
import com.chefmate.data.api.models.LoginRequest
import com.chefmate.data.api.models.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class AuthRepository {
    private val apiService = ApiClient.apiService

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Изпращаме email като usernameOrEmail (бекендът приема и email и username)
                val response = apiService.login(LoginRequest(usernameOrEmail = email, password = password))
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception("Login failed: $errorBody"))
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: e.message()
                Result.failure(Exception("HTTP error: $errorBody"))
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun register(username: String, email: String, password: String): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.register(RegisterRequest(username = username, email = email, password = password))
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception("Registration failed: $errorBody"))
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: e.message()
                Result.failure(Exception("HTTP error: $errorBody"))
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun forgotPassword(email: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.forgotPassword(ForgotPasswordRequest(email = email))
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception("Failed to send reset email: $errorBody"))
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: e.message()
                Result.failure(Exception("HTTP error: $errorBody"))
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun changePassword(token: String, currentPassword: String, newPassword: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = apiService.changePassword(
                    authToken,
                    ChangePasswordRequest(currentPassword = currentPassword, newPassword = newPassword)
                )
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception("Failed to change password: $errorBody"))
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