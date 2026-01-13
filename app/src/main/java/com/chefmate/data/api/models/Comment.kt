package com.chefmate.data.api.models

import com.google.gson.annotations.SerializedName

data class Comment(
    @SerializedName("id")
    val id: Long = 0,

    @SerializedName("content")
    val content: String = "",

    @SerializedName("userId")
    val userId: Long = 0,

    @SerializedName(value = "userName", alternate = ["username"])
    val userName: String = "",

    @SerializedName("recipeId")
    val recipeId: Long = 0,

    @SerializedName("createdAt")
    val createdAt: String = "",

    @SerializedName("parentCommentId")
    val parentCommentId: Long? = null, // For replies

    @SerializedName("replies")
    val replies: List<Comment>? = emptyList(), // Nested replies

    @SerializedName("likesCount")
    val likesCount: Int = 0,

    @SerializedName("isLiked")
    val isLiked: Boolean = false,

    @SerializedName("userProfileImageUrl")
    val userProfileImageUrl: String? = null
)