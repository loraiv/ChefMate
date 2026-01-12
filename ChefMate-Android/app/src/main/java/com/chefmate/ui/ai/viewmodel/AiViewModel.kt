package com.chefmate.ui.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chefmate.data.api.models.AiResponse
import com.chefmate.data.repository.AiRepository
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class AiViewModel(private val context: android.content.Context) : ViewModel() {
    private val tokenManager = TokenManager(context)
    private val aiRepository = AiRepository(tokenManager)

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun sendMessage(message: String, recipeId: Long? = null) {
        if (message.isBlank()) return

        // Add user message
        val userMessage = ChatMessage(message = message, isUser = true)
        _chatMessages.value = _chatMessages.value + userMessage

        // Show loading
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            aiRepository.chatWithAI(message, recipeId)
                .onSuccess { response ->
                    // Add AI response
                    val aiMessage = ChatMessage(message = response.response, isUser = false)
                    _chatMessages.value = _chatMessages.value + aiMessage
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Error communicating with AI"
                    _isLoading.value = false
                }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
    }
}

