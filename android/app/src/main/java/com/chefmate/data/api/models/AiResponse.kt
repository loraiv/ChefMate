package com.chefmate.data.api.models

import com.google.gson.annotations.SerializedName

data class AiResponse(
    @SerializedName("response")
    val response: String
)