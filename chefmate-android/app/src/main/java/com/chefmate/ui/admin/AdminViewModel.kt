package com.chefmate.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chefmate.data.api.models.UserManagementResponse
import com.chefmate.data.repository.AdminRepository
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    
    private val adminRepository = AdminRepository(TokenManager(application))
    
    private val _users = MutableStateFlow<List<UserManagementResponse>>(emptyList())
    val users: StateFlow<List<UserManagementResponse>> = _users.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun loadUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            adminRepository.getAllUsers().fold(
                onSuccess = { userList ->
                    _users.value = userList
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to load users"
                    _isLoading.value = false
                }
            )
        }
    }

    fun blockUser(userId: Long) {
        viewModelScope.launch {
            adminRepository.blockUser(userId).fold(
                onSuccess = { message ->
                    _successMessage.value = message
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to block user"
                }
            )
        }
    }

    fun unblockUser(userId: Long) {
        viewModelScope.launch {
            adminRepository.unblockUser(userId).fold(
                onSuccess = { message ->
                    _successMessage.value = message
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to unblock user"
                }
            )
        }
    }

    fun deleteUser(userId: Long) {
        viewModelScope.launch {
            adminRepository.deleteUser(userId).fold(
                onSuccess = { message ->
                    _successMessage.value = message
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to delete user"
                }
            )
        }
    }

    fun promoteToAdmin(userId: Long) {
        viewModelScope.launch {
            adminRepository.promoteToAdmin(userId).fold(
                onSuccess = { message ->
                    _successMessage.value = message
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to promote user"
                }
            )
        }
    }

    fun demoteFromAdmin(userId: Long) {
        viewModelScope.launch {
            adminRepository.demoteFromAdmin(userId).fold(
                onSuccess = { message ->
                    _successMessage.value = message
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to demote user"
                }
            )
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}
