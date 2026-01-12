package com.chefmate.ui.cooking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chefmate.data.api.models.CookingContext
import com.chefmate.data.api.models.RecipeResponse
import com.chefmate.data.repository.AiRepository
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

data class CookingSessionState(
    val recipe: RecipeResponse? = null,
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val elapsedTimeSeconds: Long = 0,
    val usedIngredients: List<String> = emptyList(),
    val cookingStage: String? = null,
    val currentAction: String? = null,
    val stoveSetting: String? = null,
    val isTimerRunning: Boolean = false,
    val startTime: Long? = null
)

class CookingModeViewModel(private val context: android.content.Context) : ViewModel() {
    private val tokenManager = TokenManager(context)
    private val aiRepository = AiRepository(tokenManager)
    
    private val _sessionState = MutableStateFlow(CookingSessionState())
    val sessionState: StateFlow<CookingSessionState> = _sessionState.asStateFlow()
    
    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var timerJob: kotlinx.coroutines.Job? = null
    
    fun startCookingSession(recipe: RecipeResponse) {
        _sessionState.value = CookingSessionState(
            recipe = recipe,
            currentStep = 0,
            totalSteps = recipe.steps.size,
            elapsedTimeSeconds = 0,
            usedIngredients = emptyList(),
            cookingStage = "preparation",
            currentAction = if (recipe.steps.isNotEmpty()) recipe.steps[0] else null,
            isTimerRunning = true,
            startTime = System.currentTimeMillis()
        )
        startTimer()
    }
    
    fun nextStep() {
        val state = _sessionState.value
        val recipe = state.recipe ?: return
        val nextStepIndex = state.currentStep + 1
        
        if (nextStepIndex < recipe.steps.size) {
            _sessionState.value = state.copy(
                currentStep = nextStepIndex,
                currentAction = recipe.steps[nextStepIndex]
            )
        }
    }
    
    fun previousStep() {
        val state = _sessionState.value
        val recipe = state.recipe ?: return
        val prevStepIndex = state.currentStep - 1
        
        if (prevStepIndex >= 0) {
            _sessionState.value = state.copy(
                currentStep = prevStepIndex,
                currentAction = recipe.steps[prevStepIndex]
            )
        }
    }
    
    fun addUsedIngredient(ingredient: String) {
        val state = _sessionState.value
        if (!state.usedIngredients.contains(ingredient)) {
            _sessionState.value = state.copy(
                usedIngredients = state.usedIngredients + ingredient
            )
        }
    }
    
    fun setCookingStage(stage: String) {
        val state = _sessionState.value
        _sessionState.value = state.copy(cookingStage = stage)
    }
    
    fun setStoveSetting(setting: String) {
        val state = _sessionState.value
        _sessionState.value = state.copy(stoveSetting = setting)
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_sessionState.value.isTimerRunning) {
                kotlinx.coroutines.delay(1000)
                val state = _sessionState.value
                val startTime = state.startTime ?: continue
                val elapsed = (System.currentTimeMillis() - startTime) / 1000
                _sessionState.value = state.copy(elapsedTimeSeconds = elapsed)
            }
        }
    }
    
    fun stopTimer() {
        timerJob?.cancel()
        val state = _sessionState.value
        _sessionState.value = state.copy(isTimerRunning = false)
    }
    
    fun sendVoiceMessage(message: String) {
        if (message.isBlank()) return
        
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            val state = _sessionState.value
            val cookingContext = CookingContext(
                currentStep = state.currentStep + 1, // 1-indexed for display
                totalSteps = state.totalSteps,
                elapsedTimeSeconds = state.elapsedTimeSeconds,
                usedIngredients = state.usedIngredients,
                cookingStage = state.cookingStage,
                currentAction = state.currentAction,
                stoveSetting = state.stoveSetting
            )
            
            aiRepository.chatWithAI(
                message = message,
                recipeId = state.recipe?.id,
                cookingContext = cookingContext
            )
                .onSuccess { response ->
                    _aiResponse.value = response.response
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
    
    fun clearAiResponse() {
        _aiResponse.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

