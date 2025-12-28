package com.chefmate.data.api.models

data class LoginRequest(
    val email: String = "",
    val password: String = ""
)