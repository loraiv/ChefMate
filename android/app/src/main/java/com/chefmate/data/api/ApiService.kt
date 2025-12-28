package com.chefmate.data.api

import com.chefmate.data.api.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ========== AUTHENTICATION ==========
    @POST("api/auth/register")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<AuthResponse>

    // ========== RECIPES ==========
    @GET("api/recipes")
    suspend fun getRecipes(
        @Header("Authorization") token: String
    ): Response<List<RecipeResponse>>

    @GET("api/recipes/{id}")
    suspend fun getRecipe(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<RecipeResponse>

    @Multipart
    @POST("api/recipes")
    suspend fun createRecipe(
        @Header("Authorization") token: String,
        @Part("recipe") recipe: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<RecipeResponse>

    @PUT("api/recipes/{id}")
    suspend fun updateRecipe(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body recipe: RecipeRequest
    ): Response<RecipeResponse>

    @DELETE("api/recipes/{id}")
    suspend fun deleteRecipe(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<Unit>

    // ========== SEARCH & FILTER ==========
    @GET("api/recipes/search")
    suspend fun searchRecipes(
        @Header("Authorization") token: String,
        @Query("query") query: String? = null,
        @Query("difficulty") difficulty: String? = null,
        @Query("maxTime") maxTime: Int? = null
    ): Response<List<RecipeResponse>>

    // ========== SHOPPING LIST ==========
    @POST("api/shopping-lists/create-from-recipe/{recipeId}")
    suspend fun createShoppingListFromRecipe(
        @Header("Authorization") token: String,
        @Path("recipeId") recipeId: Long
    ): Response<ShoppingListResponse>

    @GET("api/shopping-lists/my-list")
    suspend fun getMyShoppingList(
        @Header("Authorization") token: String
    ): Response<ShoppingListResponse>

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

    // ========== AI ASSISTANT ==========
    @POST("api/ai/chat")
    suspend fun chatWithAI(
        @Header("Authorization") token: String,
        @Body aiRequest: AiRequest
    ): Response<AiResponse>

    // ========== SOCIAL FEATURES ==========
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
}