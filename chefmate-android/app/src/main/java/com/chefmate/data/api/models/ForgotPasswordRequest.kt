package com.chefmate.data.api.models

import com.google.gson.annotations.SerializedName

data class ForgotPasswordRequest(
    @SerializedName("email")
    val email: String = ""
)

