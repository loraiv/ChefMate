package com.chefmate.data.repository

import com.chefmate.data.api.ApiClient
import com.chefmate.data.api.models.AuthResponse
import com.chefmate.data.api.models.ChangePasswordRequest
import com.chefmate.data.api.models.ForgotPasswordRequest
import com.chefmate.data.api.models.LoginRequest
import com.chefmate.data.api.models.RegisterRequest
import com.chefmate.data.api.models.ResetPasswordRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File

class AuthRepository {
    private val apiService = ApiClient.apiService

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
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

    suspend fun resetPassword(token: String, newPassword: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("AuthRepository", "Sending reset password request with token")
                val response = apiService.resetPassword(ResetPasswordRequest(token = token, newPassword = newPassword))
                android.util.Log.d("AuthRepository", "Response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
                
                if (response.isSuccessful) {
                    val body = response.body()
                    android.util.Log.d("AuthRepository", "Response body: $body")
                    Result.success(Unit)
                } else {
                    val errorBody = try {
                        response.errorBody()?.string() ?: response.message()
                    } catch (e: Exception) {
                        "Unknown error (code: ${response.code()})"
                    }
                    android.util.Log.e("AuthRepository", "Failed to reset password: $errorBody")
                    Result.failure(Exception("Failed to reset password: $errorBody"))
                }
            } catch (e: HttpException) {
                val errorBody = try {
                    e.response()?.errorBody()?.string() ?: e.message()
                } catch (ex: Exception) {
                    e.message ?: "Unknown HTTP error"
                }
                android.util.Log.e("AuthRepository", "HTTP error: $errorBody", e)
                Result.failure(Exception("HTTP error: $errorBody"))
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Network error: ${e.message}", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun forgotPassword(email: String): Result<Map<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("AuthRepository", "Sending forgot password request for: $email")
                val response = apiService.forgotPassword(ForgotPasswordRequest(email = email))
                android.util.Log.d("AuthRepository", "Response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
                
                if (response.isSuccessful) {
                    val body = response.body()
                    android.util.Log.d("AuthRepository", "Response body: $body")
                    Result.success(body ?: emptyMap())
                } else {
                    val errorBody = try {
                        response.errorBody()?.string() ?: response.message()
                    } catch (e: Exception) {
                        "Unknown error (code: ${response.code()})"
                    }
                    android.util.Log.e("AuthRepository", "Failed to send reset email: $errorBody")
                    Result.failure(Exception("Failed to send reset email: $errorBody"))
                }
            } catch (e: HttpException) {
                val errorBody = try {
                    e.response()?.errorBody()?.string() ?: e.message()
                } catch (ex: Exception) {
                    e.message ?: "Unknown HTTP error"
                }
                android.util.Log.e("AuthRepository", "HTTP error: $errorBody", e)
                Result.failure(Exception("HTTP error: $errorBody"))
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Network error: ${e.message}", e)
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

    suspend fun uploadProfileImage(token: String, imageFile: File): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
                
                val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
                
                val response = apiService.uploadProfileImage(authToken, imagePart)
                if (response.isSuccessful && response.body() != null) {
                    val imageUrl = response.body()!!["imageUrl"] as? String
                    Result.success(imageUrl ?: "")
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception("Failed to upload profile image: $errorBody"))
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: e.message()
                Result.failure(Exception("HTTP error: $errorBody"))
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun deleteProfileImage(token: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = apiService.deleteProfileImage(authToken)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception("Failed to delete profile image: $errorBody"))
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: e.message()
                Result.failure(Exception("HTTP error: $errorBody"))
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun deleteAccount(token: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = apiService.deleteAccount(authToken)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception("Failed to delete account: $errorBody"))
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