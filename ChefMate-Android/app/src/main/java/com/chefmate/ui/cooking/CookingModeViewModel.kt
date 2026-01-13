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
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong

data class CookingSessionState(
    val recipe: RecipeResponse? = null,
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val usedIngredients: List<String> = emptyList(),
    val cookingStage: String? = null,
    val currentAction: String? = null,
    val stoveSetting: String? = null,
    // Timer state
    val timerSecondsRemaining: Long = 0,
    val isTimerRunning: Boolean = false,
    val isTimerPaused: Boolean = false,
    val isTimerFinished: Boolean = false,
    val timerLabel: String? = null
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
    
    private var localTimerJob: kotlinx.coroutines.Job? = null
    
    fun startCookingSession(recipe: RecipeResponse) {
        _sessionState.value = CookingSessionState(
            recipe = recipe,
            currentStep = 0,
            totalSteps = recipe.steps.size,
            usedIngredients = emptyList(),
            cookingStage = "preparation",
            currentAction = if (recipe.steps.isNotEmpty()) recipe.steps[0] else null
        )
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
    
    fun startTimer(seconds: Long, label: String? = null) {
        try {
            val state = _sessionState.value
            _sessionState.value = state.copy(
                timerSecondsRemaining = seconds,
                isTimerRunning = true,
                isTimerPaused = false,
                isTimerFinished = false,
                timerLabel = label
            )
            
            // Start local timer for UI updates
            startLocalTimer(seconds)
            
            // Start foreground service for background timer
            com.chefmate.service.TimerService.startTimer(context, seconds, label)
        } catch (e: Exception) {
            android.util.Log.e("CookingModeViewModel", "Error starting timer", e)
            // Update state to reflect error
            val state = _sessionState.value
            _sessionState.value = state.copy(
                isTimerRunning = false,
                isTimerPaused = false,
                timerSecondsRemaining = 0
            )
            _error.value = "Failed to start timer: ${e.message}"
        }
    }
    
    fun pauseTimer() {
        val state = _sessionState.value
        if (state.isTimerRunning && !state.isTimerPaused) {
            localTimerJob?.cancel()
            _sessionState.value = state.copy(isTimerPaused = true)
            com.chefmate.service.TimerService.pauseTimer(context)
        }
    }
    
    fun resumeTimer() {
        val state = _sessionState.value
        if (state.isTimerRunning && state.isTimerPaused) {
            startLocalTimer(state.timerSecondsRemaining)
            _sessionState.value = state.copy(isTimerPaused = false)
            com.chefmate.service.TimerService.resumeTimer(context)
        }
    }
    
    private fun startLocalTimer(initialSeconds: Long) {
        localTimerJob?.cancel()
        var remaining = initialSeconds
        
        localTimerJob = viewModelScope.launch {
            while (remaining > 0) {
                delay(1000)
                
                // Check if paused
                var state = _sessionState.value
                while (state.isTimerPaused) {
                    delay(100)
                    state = _sessionState.value
                }
                
                remaining--
                
                state = _sessionState.value
                if (state.isTimerRunning && !state.isTimerPaused) {
                    _sessionState.value = state.copy(timerSecondsRemaining = remaining)
                    
                    // If timer reached 0, mark as finished
                    if (remaining <= 0) {
                        _sessionState.value = state.copy(
                            isTimerRunning = false,
                            isTimerPaused = false,
                            isTimerFinished = true,
                            timerSecondsRemaining = 0
                        )
                    }
                } else {
                    // Timer was stopped or paused externally
                    break
                }
            }
        }
    }
    
    fun stopTimer() {
        localTimerJob?.cancel()
        localTimerJob = null
        
        val state = _sessionState.value
        _sessionState.value = state.copy(
            isTimerRunning = false,
            isTimerPaused = false,
            isTimerFinished = false,
            timerSecondsRemaining = 0
        )
        // Stop foreground service
        com.chefmate.service.TimerService.stopTimer(context)
    }
    
    fun updateTimerRemaining(seconds: Long) {
        // This is called from BroadcastReceiver to sync with service
        // The local timer handles the countdown, but we sync if there's a discrepancy
        val state = _sessionState.value
        if (state.isTimerRunning) {
            val currentRemaining = state.timerSecondsRemaining
            // Only update if there's a significant difference (more than 2 seconds)
            // This prevents conflicts with local timer
            if (kotlin.math.abs(currentRemaining - seconds) > 2) {
                android.util.Log.d("CookingModeViewModel", "Syncing timer: $currentRemaining -> $seconds seconds")
                _sessionState.value = state.copy(timerSecondsRemaining = seconds)
                
                // Restart local timer with synced value
                localTimerJob?.cancel()
                startLocalTimer(seconds)
            }
            
            // If timer reached 0, stop it
            if (seconds <= 0) {
                localTimerJob?.cancel()
                _sessionState.value = state.copy(
                    isTimerRunning = false,
                    timerSecondsRemaining = 0
                )
            }
        }
    }
    
    fun sendVoiceMessage(message: String) {
        if (message.isBlank()) return
        
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            val state = _sessionState.value
            val recipe = state.recipe
            
            // Create cooking context with full recipe information
            val cookingContext = CookingContext(
                currentStep = state.currentStep + 1, // 1-indexed for display
                totalSteps = state.totalSteps,
                usedIngredients = state.usedIngredients,
                cookingStage = state.cookingStage,
                currentAction = state.currentAction,
                stoveSetting = state.stoveSetting,
                // Full recipe data for AI
                recipeTitle = recipe?.title,
                recipeDescription = recipe?.description,
                recipeIngredients = recipe?.ingredients,
                recipeSteps = recipe?.steps,
                recipeDifficulty = recipe?.difficulty,
                prepTime = recipe?.prepTime,
                cookTime = recipe?.cookTime,
                totalTime = recipe?.totalTime,
                servings = recipe?.servings
            )
            
            aiRepository.chatWithAI(
                message = message,
                recipeId = recipe?.id,
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
        localTimerJob?.cancel()
    }
}

