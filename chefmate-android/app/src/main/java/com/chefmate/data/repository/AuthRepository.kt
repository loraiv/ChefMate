package com.chefmate.data.repository

import com.chefmate.data.api.ApiClient
import com.chefmate.data.api.ApiService
import com.chefmate.data.api.models.AuthResponse
import com.chefmate.data.api.models.ChangePasswordRequest
import com.chefmate.data.api.models.ChangeUsernameRequest
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

class AuthRepository(
    private val apiService: ApiService = ApiClient.apiService
) {

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("AuthRepository", "Attempting login for: $email")
                android.util.Log.d("AuthRepository", "BASE_URL: ${ApiClient.BASE_URL}")
                val response = apiService.login(LoginRequest(usernameOrEmail = email, password = password))
                android.util.Log.d("AuthRepository", "Response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
                if (response.isSuccessful && response.body() != null) {
                    android.util.Log.d("AuthRepository", "Login successful")
                    Result.success(response.body()!!)
                } else {
                    val errorBody = try {
                        response.errorBody()?.string() ?: response.message()
                    } catch (e: Exception) {
                        "Unknown error (code: ${response.code()})"
                    }
                    android.util.Log.e("AuthRepository", "Login failed: $errorBody")
                    Result.failure(Exception("Login failed: $errorBody"))
                }
            } catch (e: java.net.UnknownHostException) {
                android.util.Log.e("AuthRepository", "Unknown host exception: ${e.message}", e)
                Result.failure(Exception("Cannot connect to server. Please check if the backend is running and the URL is correct (${ApiClient.BASE_URL})"))
            } catch (e: java.net.ConnectException) {
                android.util.Log.e("AuthRepository", "Connection exception: ${e.message}", e)
                Result.failure(Exception("Cannot connect to server at ${ApiClient.BASE_URL}. Please check if the backend is running on port 8090"))
            } catch (e: java.net.SocketTimeoutException) {
                android.util.Log.e("AuthRepository", "Timeout exception: ${e.message}", e)
                Result.failure(Exception("Connection timeout. Please check your internet connection and try again"))
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
                val errorMessage = when {
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> 
                        "Cannot connect to server. If using a real device, make sure you're using your computer's IP address instead of 10.0.2.2"
                    e.message?.contains("Connection refused", ignoreCase = true) == true -> 
                        "Connection refused. Please check if the backend server is running on port 8090"
                    else -> 
                        "Network error: ${e.message}. Please check your internet connection and ensure the backend server is running"
                }
                Result.failure(Exception(errorMessage))
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

    suspend fun changeUsername(token: String, newUsername: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = apiService.changeUsername(
                    authToken,
                    ChangeUsernameRequest(newUsername = newUsername)
                )
                if (response.isSuccessful) {
                    Result.success(newUsername)
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    Result.failure(Exception("Failed to change username: $errorBody"))
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