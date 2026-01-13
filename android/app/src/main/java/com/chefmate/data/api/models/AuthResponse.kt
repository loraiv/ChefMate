package com.chefmate.data.api.models

data class AuthResponse(
    val token: String = "",
    val userId: Long = 0,
    val email: String = "",
    val name: String = ""
)