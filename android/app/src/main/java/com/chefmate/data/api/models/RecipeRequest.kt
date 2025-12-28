package com.chefmate.data.api.models

import com.google.gson.annotations.SerializedName

data class RecipeRequest(
    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("difficulty")
    val difficulty: String, // "EASY", "MEDIUM", "HARD"

    @SerializedName("preparationTime")
    val preparationTime: Int, // в минути

    @SerializedName("ingredients")
    val ingredients: List<Ingredient>,

    @SerializedName("steps")
    val steps: List<String>
)