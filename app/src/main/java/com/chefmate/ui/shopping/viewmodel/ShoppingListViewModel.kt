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
                    _error.value = exception.message ?: "Грешка при зареждане на списъка"
                }

            _isLoading.value = false
        }
    }

    fun updateItem(item: ShoppingListItem, checked: Boolean) {
        viewModelScope.launch {
            val list = _shoppingList.value ?: return@launch
            shoppingRepository.updateShoppingListItem(list.id, item.id ?: return@launch, checked)
                .onSuccess {
                    _message.value = if (checked) "Продуктът е маркиран като закупен" else "Маркировката е премахната"
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Грешка при обновяване на продукта"
                }
        }
    }

    fun deleteItem(item: ShoppingListItem) {
        viewModelScope.launch {
            val list = _shoppingList.value ?: return@launch
            shoppingRepository.deleteShoppingListItem(list.id, item.id ?: return@launch)
                .onSuccess {
                    _message.value = "Продуктът е премахнат"
                    // Reload list to get updated items
                    loadShoppingList()
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Грешка при изтриване на продукта"
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

