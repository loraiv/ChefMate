package com.chefmate.data.api.models

import com.google.gson.annotations.SerializedName

data class RecipeResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("difficulty")
    val difficulty: String,

    @SerializedName("prepTime")
    val prepTime: Int? = null,

    @SerializedName("cookTime")
    val cookTime: Int? = null,

    @SerializedName("totalTime")
    val totalTime: Int? = null,

    @SerializedName("servings")
    val servings: Int? = null,

    @SerializedName("imageUrl")
    val imageUrl: String?,

    @SerializedName("imageUrls")
    val imageUrls: List<String>? = null,

    @SerializedName("ingredients")
    val ingredients: List<String>,

    @SerializedName("steps")
    val steps: List<String>,

    @SerializedName("userId")
    val userId: Long,

    @SerializedName("username")
    val username: String? = null,

    @SerializedName("userProfileImageUrl")
    val userProfileImageUrl: String? = null,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String,

    @SerializedName("likesCount")
    val likesCount: Int = 0,

    @SerializedName("viewsCount")
    val viewsCount: Int = 0,

    @SerializedName("isLiked")
    val isLiked: Boolean = false
)