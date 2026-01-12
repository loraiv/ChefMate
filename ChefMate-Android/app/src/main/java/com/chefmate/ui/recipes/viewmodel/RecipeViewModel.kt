package com.chefmate.ui.recipes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chefmate.data.repository.RecipeRepository
import com.chefmate.data.api.models.RecipeResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipeViewModel(
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _recipes = MutableStateFlow<List<RecipeResponse>>(emptyList())
    val recipes: StateFlow<List<RecipeResponse>> = _recipes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow<String?>(null)
    val selectedDifficulty: StateFlow<String?> = _selectedDifficulty.asStateFlow()

    private val _selectedMaxTime = MutableStateFlow<Int?>(null)
    val selectedMaxTime: StateFlow<Int?> = _selectedMaxTime.asStateFlow()

    init {
        loadRecipes()
    }

    fun loadRecipes() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            recipeRepository.getRecipes()
                .onSuccess { recipes ->
                    _recipes.value = recipes
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Error loading recipes"
                }

            _isLoading.value = false
        }
    }

    fun searchRecipes(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            recipeRepository.searchRecipes(
                query = if (query.isBlank()) null else query,
                difficulty = _selectedDifficulty.value,
                maxTime = _selectedMaxTime.value
            )
                .onSuccess { recipes ->
                    _recipes.value = recipes
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Error searching"
                }

            _isLoading.value = false
        }
    }

    fun filterByDifficulty(difficulty: String?) {
        _selectedDifficulty.value = difficulty
        applyFilters()
    }

    fun filterByTime(maxTime: Int?) {
        _selectedMaxTime.value = maxTime
        applyFilters()
    }

    private fun applyFilters() {
        viewModelScope.launch {
            _isLoading.value = true

            recipeRepository.searchRecipes(
                query = null,
                difficulty = _selectedDifficulty.value,
                maxTime = _selectedMaxTime.value
            )
                .onSuccess { recipes ->
                    _recipes.value = recipes
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Error filtering"
                }

            _isLoading.value = false
        }
    }

    fun toggleLike(recipe: RecipeResponse) {
        viewModelScope.launch {
            if (recipe.isLiked) {
                recipeRepository.unlikeRecipe(recipe.id)
                    .onSuccess {
                        // Update local state
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
                        // Update local state
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

