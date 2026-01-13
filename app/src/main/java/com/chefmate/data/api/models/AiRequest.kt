package com.chefmate.data.api.models

import com.google.gson.annotations.SerializedName

data class AiRequest(
    @SerializedName("message")
    val message: String,

    @SerializedName("recipeId")
    val recipeId: Long? = null
)