package com.chefmate.data.api.models

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("token")
    val token: String = "",
    
    @SerializedName("type")
    val type: String = "Bearer",
    
    @SerializedName("id")
    val userId: Long = 0,
    
    @SerializedName("username")
    val username: String = "",
    
    @SerializedName("email")
    val email: String = "",
    
    @SerializedName("firstName")
    val firstName: String? = null,
    
    @SerializedName("lastName")
    val lastName: String? = null,
    
    @SerializedName("role")
    val role: String? = null
)