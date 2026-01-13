package com.chefmate.data.api.models

import com.google.gson.annotations.SerializedName

data class Ingredient(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("name")
    val name: String,

    @SerializedName("quantity")
    val quantity: String,

    @SerializedName("unit")
    val unit: String? = null
)