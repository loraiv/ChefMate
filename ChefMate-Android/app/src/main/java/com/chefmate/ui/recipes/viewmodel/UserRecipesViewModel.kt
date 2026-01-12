package com.chefmate.ui.recipes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chefmate.data.repository.RecipeRepository
import com.chefmate.data.api.models.RecipeResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserRecipesViewModel(
    private val recipeRepository: RecipeRepository,
    val userId: Long,
    val username: String
) : ViewModel() {

    private val _recipes = MutableStateFlow<List<RecipeResponse>>(emptyList())
    val recipes: StateFlow<List<RecipeResponse>> = _recipes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadUserRecipes() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            recipeRepository.getUserRecipes(userId)
                .onSuccess { recipes ->
                    _recipes.value = recipes
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Error loading recipes"
                }

            _isLoading.value = false
        }
    }

    fun toggleLike(recipe: RecipeResponse) {
        viewModelScope.launch {
            if (recipe.isLiked) {
                recipeRepository.unlikeRecipe(recipe.id)
                    .onSuccess {
                        _recipes.value = _recipes.value.map {
                            if (it.id == recipe.id) {
                                it.copy(
                                    isLiked = false,
                                    likesCount = maxOf(0, it.likesCount - 1)
                                )
                            } else it
                        }
                    }
            } else {
                recipeRepository.likeRecipe(recipe.id)
                    .onSuccess {
                        _recipes.value = _recipes.value.map {
                            if (it.id == recipe.id) {
                                it.copy(
                                    isLiked = true,
                                    likesCount = it.likesCount + 1
                                )
                            } else it
                        }
                    }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

class UserRecipesViewModelFactory(
    private val recipeRepository: RecipeRepository,
    private val userId: Long,
    private val username: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserRecipesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserRecipesViewModel(recipeRepository, userId, username) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

