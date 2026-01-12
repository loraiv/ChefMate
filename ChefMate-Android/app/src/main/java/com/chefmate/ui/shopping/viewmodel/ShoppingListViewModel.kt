package com.chefmate.ui.shopping.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chefmate.data.api.models.ShoppingListItem
import com.chefmate.data.api.models.ShoppingListResponse
import com.chefmate.data.repository.ShoppingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShoppingListViewModel(
    private val shoppingRepository: ShoppingRepository
) : ViewModel() {

    private val _shoppingList = MutableStateFlow<ShoppingListResponse?>(null)
    val shoppingList: StateFlow<ShoppingListResponse?> = _shoppingList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        // Observe shopping list from repository
        viewModelScope.launch {
            shoppingRepository.shoppingList.collect { list ->
                _shoppingList.value = list
            }
        }
    }

    fun loadShoppingList() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            shoppingRepository.getMyShoppingList()
                .onSuccess {
                    // List is already updated via flow
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Error loading list"
                }

            _isLoading.value = false
        }
    }

    fun updateItem(item: ShoppingListItem, checked: Boolean) {
        viewModelScope.launch {
            val list = _shoppingList.value ?: return@launch
            shoppingRepository.updateShoppingListItem(list.id, item.id ?: return@launch, checked)
                .onSuccess {
                    _message.value = if (checked) "Product marked as purchased" else "Mark removed"
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Error updating product"
                }
        }
    }

    fun addItem(name: String, quantity: String = "", unit: String = "") {
        viewModelScope.launch {
            val list = _shoppingList.value ?: return@launch
            shoppingRepository.addShoppingListItem(list.id, name, quantity, unit)
                .onSuccess {
                    _message.value = "Product added"
                    loadShoppingList()
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Error adding product"
                }
        }
    }

    fun deleteItem(item: ShoppingListItem) {
        viewModelScope.launch {
            val list = _shoppingList.value ?: return@launch
            shoppingRepository.deleteShoppingListItem(list.id, item.id ?: return@launch)
                .onSuccess {
                    _message.value = "Product removed"
                    loadShoppingList()
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Error deleting product"
                }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearMessage() {
        _message.value = null
    }
}

class ShoppingListViewModelFactory(
    private val shoppingRepository: ShoppingRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShoppingListViewModel(shoppingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

