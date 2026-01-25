package com.chefmate.ui.recipes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chefmate.data.repository.RecipeRepository
import com.chefmate.data.api.models.RecipeResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LikedRecipesViewModel(
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _recipes = MutableStateFlow<List<RecipeResponse>>(emptyList())
    val recipes: StateFlow<List<RecipeResponse>> = _recipes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadLikedRecipes() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            recipeRepository.getRecipes()
                .onSuccess { allRecipes ->
                    // Filter only liked recipes
                    _recipes.value = allRecipes.filter { it.isLiked }
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Error loading liked recipes"
                }

            _isLoading.value = false
        }
    }

    fun toggleLike(recipe: RecipeResponse) {
        viewModelScope.launch {
            if (recipe.isLiked) {
                recipeRepository.unlikeRecipe(recipe.id)
                    .onSuccess {
                        // Remove from list if unliked
                        _recipes.value = _recipes.value.filter { it.id != recipe.id }
                    }
            } else {
                recipeRepository.likeRecipe(recipe.id)
                    .onSuccess {
                        loadLikedRecipes()
                    }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

class LikedRecipesViewModelFactory(
    private val recipeRepository: RecipeRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LikedRecipesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LikedRecipesViewModel(recipeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

