package com.chefmate.data.api.models

import com.google.gson.annotations.SerializedName

data class ShoppingListResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("userId")
    val userId: Long,

    @SerializedName("items")
    val items: List<ShoppingListItem>,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("name")
    val name: String = "Shopping List"
)