package com.chefmate.data.api.models

import com.google.gson.annotations.SerializedName

data class AiRequest(
    @SerializedName("message")
    val message: String,

    @SerializedName("recipeId")
    val recipeId: Long? = null,
    
    @SerializedName("cookingContext")
    val cookingContext: CookingContext? = null
)

data class CookingContext(
    @SerializedName("currentStep")
    val currentStep: Int,
    
    @SerializedName("totalSteps")
    val totalSteps: Int,
    
    @SerializedName("usedIngredients")
    val usedIngredients: List<String>,
    
    @SerializedName("cookingStage")
    val cookingStage: String? = null, // "preparation", "cooking", "baking", "serving", etc.
    
    @SerializedName("currentAction")
    val currentAction: String? = null,
    
    @SerializedName("stoveSetting")
    val stoveSetting: String? = null, // "Low", "Medium", "High", etc.
    
    // Full recipe information
    @SerializedName("recipeTitle")
    val recipeTitle: String? = null,
    
    @SerializedName("recipeDescription")
    val recipeDescription: String? = null,
    
    @SerializedName("recipeIngredients")
    val recipeIngredients: List<String>? = null,
    
    @SerializedName("recipeSteps")
    val recipeSteps: List<String>? = null,
    
    @SerializedName("recipeDifficulty")
    val recipeDifficulty: String? = null,
    
    @SerializedName("prepTime")
    val prepTime: Int? = null,
    
    @SerializedName("cookTime")
    val cookTime: Int? = null,
    
    @SerializedName("totalTime")
    val totalTime: Int? = null,
    
    @SerializedName("servings")
    val servings: Int? = null
)