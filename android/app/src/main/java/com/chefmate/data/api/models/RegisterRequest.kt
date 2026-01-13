package com.chefmate.data.api.models

data class RegisterRequest(
    val name: String = "",
    val email: String = "",
    val password: String = ""
)