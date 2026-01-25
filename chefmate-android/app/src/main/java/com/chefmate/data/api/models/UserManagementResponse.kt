package com.chefmate.data.api.models

import com.google.gson.annotations.SerializedName

data class UserManagementResponse(
    @SerializedName("id")
    val id: Long,
    @SerializedName("username")
    val username: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("role")
    val role: String,
    @SerializedName("enabled")
    val enabled: Boolean,
    @SerializedName("hasPassword")
    val hasPassword: Boolean? = null
)
