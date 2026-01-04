package com.chefmate.data.repository

import com.chefmate.data.api.ApiService
import com.chefmate.data.api.models.ShoppingListResponse
import com.chefmate.data.api.models.ShoppingListItem
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ShoppingRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    private val _shoppingList = MutableStateFlow<ShoppingListResponse?>(null)
    val shoppingList: StateFlow<ShoppingListResponse?> = _shoppingList.asStateFlow()

    private fun getAuthToken(): String {
        val token = tokenManager.getToken()
        return if (token.isNullOrEmpty()) {
            ""
        } else {
            "Bearer $token"
        }
    }

    suspend fun createShoppingListFromRecipe(recipeId: Long): Result<ShoppingListResponse> {
        return try {
            val token = getAuthToken()
            if (token.isBlank()) {
                return Result.failure(Exception("Не сте влезли в системата"))
            }
            
            val response = apiService.createShoppingListFromRecipe(token, recipeId)

            if (response.isSuccessful && response.body() != null) {
                val shoppingList = response.body()!!
                _shoppingList.value = shoppingList
                Result.success(shoppingList)
            } else {
                val errorMessage = when (response.code()) {
                    401 -> "Не сте авторизирани"
                    404 -> "Рецептата не е намерена"
                    else -> "Грешка при създаване на списък за пазаруване"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Грешка при добавяне в списъка: ${e.message}"))
        }
    }

    suspend fun getMyShoppingList(): Result<ShoppingListResponse> {
        return try {
            val token = getAuthToken()
            val response = apiService.getMyShoppingList(token)

            if (response.isSuccessful) {
                val shoppingList = response.body()!!
                _shoppingList.value = shoppingList
                Result.success(shoppingList)
            } else {
                Result.failure(Exception("Failed to fetch shopping list"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateShoppingListItem(
        listId: Long,
        itemId: Long,
        checked: Boolean
    ): Result<ShoppingListItem> {
        return try {
            val token = getAuthToken()
            // First get current item
            val currentList = _shoppingList.value
            val currentItem = currentList?.items?.find { it.id == itemId }

            if (currentItem == null) {
                return Result.failure(Exception("Item not found"))
            }

            val updatedItem = currentItem.copy(purchased = checked)
            val response = apiService.updateShoppingListItem(token, listId, itemId, updatedItem)

            if (response.isSuccessful) {
                // Update local state
                val newItem = response.body()!!
                val updatedItems = currentList.items.map { item ->
                    if (item.id == itemId) newItem else item
                }
                _shoppingList.value = currentList.copy(items = updatedItems)
                Result.success(newItem)
            } else {
                Result.failure(Exception("Failed to update item"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteShoppingListItem(listId: Long, itemId: Long): Result<Boolean> {
        return try {
            val token = getAuthToken()
            val response = apiService.deleteShoppingListItem(token, listId, itemId)

            if (response.isSuccessful) {
                // Update local state
                val currentList = _shoppingList.value
                if (currentList != null) {
                    val updatedItems = currentList.items.filter { it.id != itemId }
                    _shoppingList.value = currentList.copy(items = updatedItems)
                }
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to delete item"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}