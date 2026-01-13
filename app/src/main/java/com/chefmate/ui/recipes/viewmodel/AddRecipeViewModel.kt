package com.chefmate.ui.recipes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chefmate.data.api.models.RecipeResponse
import com.chefmate.data.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddRecipeViewModel(
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _recipeCreated = MutableStateFlow<RecipeResponse?>(null)
    val recipeCreated: StateFlow<RecipeResponse?> = _recipeCreated.asStateFlow()

    fun createRecipe(
        title: String,
        description: String,
        difficulty: String,
        prepTime: Int?,
        cookTime: Int?,
        servings: Int?,
        ingredients: List<String>,
        steps: List<String>,
        imagePaths: List<String>?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val recipeRequest = com.chefmate.data.api.models.RecipeRequest(
                title = title,
                description = description,
                difficulty = difficulty,
                prepTime = prepTime,
                cookTime = cookTime,
                servings = servings,
                ingredients = ingredients,
                steps = steps
            )

            recipeRepository.createRecipe(recipeRequest, imagePaths)
                .onSuccess { recipe ->
                    _recipeCreated.value = recipe
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Грешка при създаване на рецепта"
                }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearRecipeCreated() {
        _recipeCreated.value = null
    }
}

class AddRecipeViewModelFactory(
    private val recipeRepository: RecipeRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddRecipeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddRecipeViewModel(recipeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

