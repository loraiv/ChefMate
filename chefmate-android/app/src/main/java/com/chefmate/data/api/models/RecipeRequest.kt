package com.chefmate.data.api.models

import com.google.gson.annotations.SerializedName

data class RecipeRequest(
    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("difficulty")
    val difficulty: String,

    @SerializedName("prepTime")
    val prepTime: Int? = null,

    @SerializedName("cookTime")
    val cookTime: Int? = null,

    @SerializedName("servings")
    val servings: Int? = null,

    @SerializedName("ingredients")
    val ingredients: List<String>,

    @SerializedName("steps")
    val steps: List<String>
)