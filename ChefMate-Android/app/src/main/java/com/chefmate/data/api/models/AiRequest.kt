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
    
    @SerializedName("elapsedTimeSeconds")
    val elapsedTimeSeconds: Long,
    
    @SerializedName("usedIngredients")
    val usedIngredients: List<String>,
    
    @SerializedName("cookingStage")
    val cookingStage: String? = null, // "preparation", "cooking", "baking", "serving", etc.
    
    @SerializedName("currentAction")
    val currentAction: String? = null,
    
    @SerializedName("stoveSetting")
    val stoveSetting: String? = null // "Low", "Medium", "High", etc.
)