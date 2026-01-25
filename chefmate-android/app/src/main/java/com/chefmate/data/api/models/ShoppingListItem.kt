package com.chefmate.data.api.models

import com.google.gson.annotations.SerializedName

data class ShoppingListItem(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("name")
    val name: String,

    @SerializedName("quantity")
    val quantity: String? = null,

    @SerializedName("unit")
    val unit: String? = null,

    @SerializedName("purchased")
    val purchased: Boolean = false,

    @SerializedName("recipeId")
    val recipeId: Long? = null
) {
    // Backward compatibility - use purchased as checked
    val checked: Boolean
        get() = purchased
}