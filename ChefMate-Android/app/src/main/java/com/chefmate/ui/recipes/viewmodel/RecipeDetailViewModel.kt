package com.chefmate.ui.recipes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chefmate.data.repository.RecipeRepository
import com.chefmate.data.repository.ShoppingRepository
import com.chefmate.data.api.models.RecipeResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipeDetailViewModel(
    private val recipeRepository: RecipeRepository,
    private val shoppingRepository: ShoppingRepository
) : ViewModel() {

    private val _recipe = MutableStateFlow<RecipeResponse?>(null)
    val recipe: StateFlow<RecipeResponse?> = _recipe.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadRecipe(recipeId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            recipeRepository.getRecipeById(recipeId)
                .onSuccess { recipe ->
                    _recipe.value = recipe
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Error loading recipe"
                }

            _isLoading.value = false
        }
    }

    fun toggleLike(recipe: RecipeResponse) {
        viewModelScope.launch {
            if (recipe.isLiked) {
                recipeRepository.unlikeRecipe(recipe.id)
                    .onSuccess {
                        _recipe.value = recipe.copy(
                            isLiked = false,
                            likesCount = maxOf(0, recipe.likesCount - 1)
                        )
                    }
            } else {
                recipeRepository.likeRecipe(recipe.id)
                    .onSuccess {
                        _recipe.value = recipe.copy(
                            isLiked = true,
                            likesCount = recipe.likesCount + 1
                        )
                    }
            }
        }
    }

    private val _shoppingListMessage = MutableStateFlow<String?>(null)
    val shoppingListMessage: StateFlow<String?> = _shoppingListMessage.asStateFlow()

    fun addToShoppingList(recipeId: Long) {
        viewModelScope.launch {
            _shoppingListMessage.value = null
            shoppingRepository.createShoppingListFromRecipe(recipeId)
                .onSuccess { shoppingList ->
                    _shoppingListMessage.value = "Products added to shopping list"
                    shoppingRepository.getMyShoppingList()
                }
                .onFailure { exception ->
                    _shoppingListMessage.value = exception.message ?: "Error adding to shopping list"
                }
        }
    }

    fun clearShoppingListMessage() {
        _shoppingListMessage.value = null
    }

    fun deleteRecipe(recipeId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            recipeRepository.deleteRecipe(recipeId)
                .onSuccess {
                    _error.value = "Recipe deleted successfully"
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Error deleting recipe"
                }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}

class RecipeDetailViewModelFactory(
    private val recipeRepository: RecipeRepository,
    private val shoppingRepository: ShoppingRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecipeDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecipeDetailViewModel(recipeRepository, shoppingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

