package com.chefmate.data.api.models

import com.google.gson.annotations.SerializedName

data class ShoppingListResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String = "Shopping List",

    @SerializedName("username")
    val username: String? = null,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("completed")
    val completed: Boolean = false,

    @SerializedName("items")
    val items: List<ShoppingListItem> = emptyList()
)