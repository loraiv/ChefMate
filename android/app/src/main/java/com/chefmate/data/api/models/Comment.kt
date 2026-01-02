package com.chefmate.data.api.models

import com.google.gson.annotations.SerializedName

data class Comment(
    @SerializedName("id")
    val id: Long,

    @SerializedName("content")
    val content: String,

    @SerializedName("userId")
    val userId: Long,

    @SerializedName("userName")
    val userName: String,

    @SerializedName("recipeId")
    val recipeId: Long,

    @SerializedName("createdAt")
    val createdAt: String
)