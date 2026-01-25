package com.chefmate.data.api.models

import com.google.gson.annotations.SerializedName

data class ChangeUsernameRequest(
    @SerializedName("newUsername")
    val newUsername: String
)
