package com.chefmate.data.api.models

import com.google.gson.annotations.SerializedName

data class ShoppingListItem(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("name")
    val name: String,

    @SerializedName("quantity")
    val quantity: String,

    @SerializedName("checked")
    val checked: Boolean = false,

    @SerializedName("recipeId")
    val recipeId: Long? = null
)