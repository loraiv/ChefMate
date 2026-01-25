package com.chefmate.data.api

import com.chefmate.data.api.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/auth/register")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<AuthResponse>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body forgotPasswordRequest: ForgotPasswordRequest): Response<Map<String, String>>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body resetPasswordRequest: ResetPasswordRequest): Response<Map<String, String>>

    @POST("api/auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body changePasswordRequest: ChangePasswordRequest
    ): Response<Map<String, String>>

    @Multipart
    @POST("api/auth/upload-profile-image")
    suspend fun uploadProfileImage(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part
    ): Response<Map<String, String>>

    @DELETE("api/auth/delete-profile-image")
    suspend fun deleteProfileImage(
        @Header("Authorization") token: String
    ): Response<Map<String, String>>

    @DELETE("api/auth/delete-account")
    suspend fun deleteAccount(
        @Header("Authorization") token: String
    ): Response<Map<String, String>>

    @GET("api/recipes")
    suspend fun getRecipes(
        @Header("Authorization") token: String
    ): Response<List<RecipeResponse>>

    @GET("api/recipes/{id}")
    suspend fun getRecipe(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<RecipeResponse>

    @GET("api/recipes/user/{userId}")
    suspend fun getUserRecipes(
        @Header("Authorization") token: String,
        @Path("userId") userId: Long
    ): Response<List<RecipeResponse>>

    @Multipart
    @POST("api/recipes")
    suspend fun createRecipe(
        @Header("Authorization") token: String,
        @Part("recipe") recipe: RequestBody,
        @Part images: List<MultipartBody.Part>?
    ): Response<RecipeResponse>

    @Multipart
    @PUT("api/recipes/{id}")
    suspend fun updateRecipe(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Part("recipe") recipe: RequestBody,
        @Part images: List<MultipartBody.Part>?,
        @Part("existingImageUrls") existingImageUrls: RequestBody?
    ): Response<RecipeResponse>

    @DELETE("api/recipes/{id}")
    suspend fun deleteRecipe(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<Unit>

    @GET("api/recipes/search")
    suspend fun searchRecipes(
        @Header("Authorization") token: String,
        @Query("query") query: String? = null,
        @Query("difficulty") difficulty: String? = null,
        @Query("maxTime") maxTime: Int? = null
    ): Response<List<RecipeResponse>>

    @POST("api/shopping-lists/create-from-recipe/{recipeId}")
    suspend fun createShoppingListFromRecipe(
        @Header("Authorization") token: String,
        @Path("recipeId") recipeId: Long
    ): Response<ShoppingListResponse>

    @GET("api/shopping-lists/my-list")
    suspend fun getMyShoppingList(
        @Header("Authorization") token: String
    ): Response<ShoppingListResponse>

    @POST("api/shopping-lists/{listId}/items")
    suspend fun addShoppingListItem(
        @Header("Authorization") token: String,
        @Path("listId") listId: Long,
        @Body item: ShoppingListItem
    ): Response<ShoppingListItem>

    @PUT("api/shopping-lists/{listId}/items/{itemId}")
    suspend fun updateShoppingListItem(
        @Header("Authorization") token: String,
        @Path("listId") listId: Long,
        @Path("itemId") itemId: Long,
        @Body item: ShoppingListItem
    ): Response<ShoppingListItem>

    @DELETE("api/shopping-lists/{listId}/items/{itemId}")
    suspend fun deleteShoppingListItem(
        @Header("Authorization") token: String,
        @Path("listId") listId: Long,
        @Path("itemId") itemId: Long
    ): Response<Unit>

    @POST("api/ai/chat")
    suspend fun chatWithAI(
        @Header("Authorization") token: String,
        @Body aiRequest: AiRequest
    ): Response<AiResponse>

    @POST("api/recipes/{recipeId}/like")
    suspend fun likeRecipe(
        @Header("Authorization") token: String,
        @Path("recipeId") recipeId: Long
    ): Response<Unit>

    @DELETE("api/recipes/{recipeId}/like")
    suspend fun unlikeRecipe(
        @Header("Authorization") token: String,
        @Path("recipeId") recipeId: Long
    ): Response<Unit>

    @GET("api/recipes/{recipeId}/comments")
    suspend fun getRecipeComments(
        @Header("Authorization") token: String,
        @Path("recipeId") recipeId: Long
    ): Response<List<Comment>>

    @POST("api/recipes/{recipeId}/comments")
    suspend fun addComment(
        @Header("Authorization") token: String,
        @Path("recipeId") recipeId: Long,
        @Body comment: Map<String, String>
    ): Response<Comment>

    @POST("api/recipes/comments/{commentId}/reply")
    suspend fun replyToComment(
        @Header("Authorization") token: String,
        @Path("commentId") commentId: Long,
        @Body reply: Map<String, String>
    ): Response<Comment>

    @POST("api/recipes/comments/{commentId}/like")
    suspend fun likeComment(
        @Header("Authorization") token: String,
        @Path("commentId") commentId: Long
    ): Response<Unit>

    @DELETE("api/recipes/comments/{commentId}/like")
    suspend fun unlikeComment(
        @Header("Authorization") token: String,
        @Path("commentId") commentId: Long
    ): Response<Unit>

    @DELETE("api/recipes/comments/{commentId}")
    suspend fun deleteComment(
        @Header("Authorization") token: String,
        @Path("commentId") commentId: Long
    ): Response<Unit>

    // ========== CHANGE USERNAME ==========
    @PUT("api/auth/change-username")
    suspend fun changeUsername(
        @Header("Authorization") token: String,
        @Body changeUsernameRequest: ChangeUsernameRequest
    ): Response<Map<String, String>>

    // ========== ADMIN ENDPOINTS ==========
    @GET("api/admin/users")
    suspend fun getAllUsers(
        @Header("Authorization") token: String
    ): Response<List<UserManagementResponse>>

    @GET("api/admin/users/{userId}")
    suspend fun getUserById(
        @Header("Authorization") token: String,
        @Path("userId") userId: Long
    ): Response<UserManagementResponse>

    @POST("api/admin/users/{userId}/block")
    suspend fun blockUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: Long
    ): Response<Map<String, String>>

    @POST("api/admin/users/{userId}/unblock")
    suspend fun unblockUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: Long
    ): Response<Map<String, String>>

    @DELETE("api/admin/users/{userId}")
    suspend fun deleteUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: Long
    ): Response<Map<String, String>>

    @POST("api/admin/promote-to-admin/{userId}")
    suspend fun promoteToAdmin(
        @Header("Authorization") token: String,
        @Path("userId") userId: Long
    ): Response<Map<String, String>>

    @POST("api/admin/demote-from-admin/{userId}")
    suspend fun demoteFromAdmin(
        @Header("Authorization") token: String,
        @Path("userId") userId: Long
    ): Response<Map<String, String>>

    @DELETE("api/admin/recipes/{recipeId}")
    suspend fun deleteRecipeAsAdmin(
        @Header("Authorization") token: String,
        @Path("recipeId") recipeId: Long
    ): Response<Map<String, String>>

    @DELETE("api/admin/comments/{commentId}")
    suspend fun deleteCommentAsAdmin(
        @Header("Authorization") token: String,
        @Path("commentId") commentId: Long
    ): Response<Map<String, String>>
}