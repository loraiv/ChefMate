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

    @SerializedName("preparationTime")
    val preparationTime: Int,

    @SerializedName("imageUrl")
    val imageUrl: String?,

    @SerializedName("ingredients")
    val ingredients: List<Ingredient>,

    @SerializedName("steps")
    val steps: List<String>,

    @SerializedName("userId")
    val userId: Long,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String,

    @SerializedName("likesCount")
    val likesCount: Int = 0,

    @SerializedName("commentsCount")
    val commentsCount: Int = 0,

    @SerializedName("isLiked")
    val isLiked: Boolean = false
)