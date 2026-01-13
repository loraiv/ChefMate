package com.chefmate.ui.cooking

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
<<<<<<< Updated upstream
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
=======
import android.speech.tts.TextToSpeech
>>>>>>> Stashed changes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.chefmate.R
import com.chefmate.data.api.models.RecipeResponse
import com.chefmate.databinding.FragmentCookingModeBinding
import kotlinx.coroutines.launch
import java.util.Locale

class CookingModeFragment : Fragment() {

    private var _binding: FragmentCookingModeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CookingModeViewModel by viewModels {
        CookingModeViewModelFactory(requireActivity().applicationContext)
    }

<<<<<<< Updated upstream
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
=======
    private var mlKitSpeechRecognizer: MlKitSpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var autoSpeakEnabled = true // Default to enabled
    private var timerFinishedDialogShown = false
    private var previousTimerState: CookingSessionState? = null
    
    private val timerUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.chefmate.TIMER_UPDATE") {
                val remaining = intent.getLongExtra("remaining_seconds", 0)
                android.util.Log.d("CookingModeFragment", "Received timer update: $remaining seconds")
                // Update on main thread
                requireActivity().runOnUiThread {
                    if (isAdded && _binding != null) {
                        viewModel.updateTimerRemaining(remaining)
                        updateTimerUI(remaining)
                    }
                }
            }
        }
    }
>>>>>>> Stashed changes

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && isAdded) {
            startVoiceRecognition()
        } else if (isAdded) {
            Toast.makeText(requireContext(), "Microphone permission is required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            _binding = FragmentCookingModeBinding.inflate(inflater, container, false)
            binding.root
        } catch (e: Exception) {
            android.util.Log.e("CookingModeFragment", "Error inflating layout", e)
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (view == null) {
            android.util.Log.e("CookingModeFragment", "View is null in onViewCreated")
            return
        }

        try {
            val recipeId = arguments?.getLong("recipeId") ?: -1L
            android.util.Log.d("CookingModeFragment", "RecipeId: $recipeId")
            
            if (recipeId == -1L) {
                android.util.Log.w("CookingModeFragment", "Invalid recipeId")
                if (isAdded && view != null) {
                    Toast.makeText(requireContext(), "Error: Recipe not found", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                return
            }

            // Load recipe and start session
            loadRecipeAndStart(recipeId)
            setupTextToSpeech()
            setupSpeechRecognizer()
            setupClickListeners()
            setupObservers()
            
            // Register timer receiver only if fragment is added
            if (isAdded) {
                registerTimerReceiver()
            }
        } catch (e: Exception) {
            android.util.Log.e("CookingModeFragment", "Error in onViewCreated", e)
            e.printStackTrace()
            if (isAdded && view != null) {
                try {
                    Toast.makeText(requireContext(), "Error initializing cooking mode: ${e.message}", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } catch (e2: Exception) {
                    android.util.Log.e("CookingModeFragment", "Error showing error message", e2)
                }
            }
        }
    }
    
    private fun registerTimerReceiver() {
        try {
            val filter = IntentFilter("com.chefmate.TIMER_UPDATE")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireContext().registerReceiver(timerUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                requireContext().registerReceiver(timerUpdateReceiver, filter)
            }
            android.util.Log.d("CookingModeFragment", "Timer receiver registered successfully")
        } catch (e: Exception) {
            android.util.Log.e("CookingModeFragment", "Error registering timer receiver", e)
            e.printStackTrace()
        }
    }
    
    private fun setupTextToSpeech() {
        if (!isAdded) return
        
        try {
            textToSpeech = TextToSpeech(requireContext()) { status ->
                if (status == TextToSpeech.SUCCESS && isAdded) {
                    val result = textToSpeech?.setLanguage(Locale.US)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        textToSpeech?.setLanguage(Locale.getDefault())
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CookingModeFragment", "Error setting up text to speech", e)
        }
    }
    
    private fun loadRecipeAndStart(recipeId: Long) {
        if (!isAdded || _binding == null) return
        
        android.util.Log.d("CookingModeFragment", "Loading recipe with ID: $recipeId")
        
        // Show loading indicator
        binding.loadingIndicator.visibility = View.VISIBLE
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Load recipe from repository
                val tokenManager = com.chefmate.utils.TokenManager(requireContext())
                val apiService = com.chefmate.di.AppModule.provideApiService()
                val recipeRepository = com.chefmate.data.repository.RecipeRepository(apiService, tokenManager)
                
                android.util.Log.d("CookingModeFragment", "Fetching recipe from API...")
                recipeRepository.getRecipeById(recipeId)
                    .onSuccess { recipe ->
                        android.util.Log.d("CookingModeFragment", "Recipe loaded: ${recipe.title}")
                        if (isAdded && _binding != null) {
                            binding.loadingIndicator.visibility = View.GONE
                            viewModel.startCookingSession(recipe)
                            updateUI()
                        }
                    }
                    .onFailure { exception ->
                        android.util.Log.e("CookingModeFragment", "Failed to load recipe", exception)
                        if (isAdded && _binding != null) {
                            binding.loadingIndicator.visibility = View.GONE
                            Toast.makeText(requireContext(), "Error loading recipe: ${exception.message}", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                    }
            } catch (e: Exception) {
                android.util.Log.e("CookingModeFragment", "Error loading recipe", e)
                e.printStackTrace()
                if (isAdded && _binding != null) {
                    binding.loadingIndicator.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error loading recipe: ${e.message}", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun setupSpeechRecognizer() {
<<<<<<< Updated upstream
        if (SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    binding.microphoneButton.setIconResource(R.drawable.ic_pause)
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                    binding.microphoneButton.setIconResource(R.drawable.ic_mic)
                }

                override fun onError(error: Int) {
                    isListening = false
                    binding.microphoneButton.setIconResource(R.drawable.ic_mic)
                    when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> Toast.makeText(requireContext(), "Audio error", Toast.LENGTH_SHORT).show()
                        SpeechRecognizer.ERROR_CLIENT -> {}
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> Toast.makeText(requireContext(), "No microphone permission", Toast.LENGTH_SHORT).show()
                        SpeechRecognizer.ERROR_NETWORK -> Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> Toast.makeText(requireContext(), "Network timeout", Toast.LENGTH_SHORT).show()
                        SpeechRecognizer.ERROR_NO_MATCH -> Toast.makeText(requireContext(), "Speech not recognized", Toast.LENGTH_SHORT).show()
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> Toast.makeText(requireContext(), "Recognizer busy", Toast.LENGTH_SHORT).show()
                        SpeechRecognizer.ERROR_SERVER -> Toast.makeText(requireContext(), "Server error", Toast.LENGTH_SHORT).show()
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> Toast.makeText(requireContext(), "No speech detected", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val spokenText = matches[0]
                        viewModel.sendVoiceMessage(spokenText)
                    }
                    isListening = false
                    binding.microphoneButton.setIconResource(R.drawable.ic_mic)
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
=======
        if (!isAdded) return
        
        try {
            mlKitSpeechRecognizer = MlKitSpeechRecognizer(requireContext())
            
            if (!mlKitSpeechRecognizer?.isAvailable()!!) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Speech recognition is not available on this device", Toast.LENGTH_SHORT).show()
                }
                return
            }
        } catch (e: Exception) {
            android.util.Log.e("CookingModeFragment", "Error setting up speech recognizer", e)
            return
        }
        
        // Setup callbacks
        mlKitSpeechRecognizer?.onResult = { recognizedText ->
            viewModel.sendVoiceMessage(recognizedText)
            binding.microphoneButton.setIconResource(R.drawable.ic_mic)
        }
        
        mlKitSpeechRecognizer?.onError = { errorMessage ->
            // Only show critical errors, not common ones like "no speech detected"
            if (errorMessage.contains("permission", ignoreCase = true) ||
                errorMessage.contains("network", ignoreCase = true) ||
                errorMessage.contains("audio", ignoreCase = true) ||
                errorMessage.contains("server", ignoreCase = true)) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
            // For "no speech detected" or "no match" errors, just silently reset
            binding.microphoneButton.setIconResource(R.drawable.ic_mic)
        }
        
        mlKitSpeechRecognizer?.onListeningStateChanged = { isListening ->
            if (isListening) {
                binding.microphoneButton.setIconResource(R.drawable.ic_pause)
            } else {
                binding.microphoneButton.setIconResource(R.drawable.ic_mic)
            }
>>>>>>> Stashed changes
        }
    }

    private fun setupClickListeners() {
        binding.microphoneButton.setOnClickListener {
            if (isListening) {
                stopVoiceRecognition()
            } else {
                checkPermissionAndStartVoiceRecognition()
            }
        }
        
        // Next/Previous step buttons
        binding.nextStepButton.setOnClickListener {
            viewModel.nextStep()
        }
        
        binding.previousStepButton.setOnClickListener {
            viewModel.previousStep()
        }
        
        // Auto-speak switch
        binding.autoSpeakSwitch.isChecked = autoSpeakEnabled
        binding.autoSpeakSwitch.setOnCheckedChangeListener { _, isChecked ->
            autoSpeakEnabled = isChecked
        }
        
        // Timer button
        binding.timerButton.setOnClickListener {
            showTimerDialog()
        }
        
        // Pause/Resume timer button
        binding.pauseResumeTimerButton.setOnClickListener {
            val state = viewModel.sessionState.value
            if (state.isTimerPaused) {
                viewModel.resumeTimer()
            } else {
                viewModel.pauseTimer()
            }
        }
        
        // Stop timer button
        binding.stopTimerButton.setOnClickListener {
            viewModel.stopTimer()
        }
        
        // Stop alarm button (when timer finished)
        binding.stopAlarmButton.setOnClickListener {
            viewModel.stopTimer()
        }
    }
    
    private fun showTimerDialog() {
        val dialog = TimerDialog()
        dialog.onTimerSet = { minutes, seconds, label ->
            val totalSeconds = minutes * 60L + seconds
            viewModel.startTimer(totalSeconds, label)
        }
        dialog.show(childFragmentManager, "TimerDialog")
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessionState.collect { state ->
                val prevState = previousTimerState
                previousTimerState = state
                
                updateUI()
                updateTimerUI(state.timerSecondsRemaining)
                updateTimerControls(state.isTimerRunning, state.isTimerPaused, state.isTimerFinished, state.timerLabel)
                
                // Show timer finished dialog when timer reaches 0 and was running
                if (prevState != null &&
                    prevState.isTimerRunning && 
                    prevState.timerSecondsRemaining > 0 &&
                    !state.isTimerRunning && 
                    state.timerSecondsRemaining == 0L &&
                    state.isTimerFinished) {
                    showTimerFinishedDialog(state.timerLabel)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.aiResponse.collect { response ->
                if (response != null) {
                    binding.aiResponseCard.visibility = View.VISIBLE
                    binding.aiResponseText.text = response
                    // Automatically speak AI response only if enabled
                    if (autoSpeakEnabled) {
                        speakText(response)
                    }
                } else {
                    binding.aiResponseCard.visibility = View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.microphoneButton.isEnabled = !isLoading
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    if (isAdded) {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun updateUI() {
        if (!isAdded || _binding == null) {
            android.util.Log.w("CookingModeFragment", "Cannot update UI - fragment not added or binding is null")
            return
        }
        
        val state = viewModel.sessionState.value
        val recipe = state.recipe
        
        if (recipe == null) {
            android.util.Log.w("CookingModeFragment", "Recipe is null, cannot update UI")
            binding.recipeTitle.text = "Loading recipe..."
            return
        }

        try {
            android.util.Log.d("CookingModeFragment", "Updating UI with recipe: ${recipe.title}")
            
            // Recipe title
            binding.recipeTitle.text = recipe.title

            // Current step
            binding.currentStepText.text = "Step: ${state.currentStep + 1}/${state.totalSteps}"

            // Current action - show current step content
            val currentStepText = if (state.currentStep < recipe.steps.size) {
                recipe.steps[state.currentStep]
            } else {
                "No action"
            }
            binding.currentActionText.text = "Action: $currentStepText"

            // Needed products (extract from current step or use recipe ingredients)
            val neededProducts = if (state.currentStep < recipe.ingredients.size) {
                recipe.ingredients[state.currentStep]
            } else {
                recipe.ingredients.joinToString(", ")
            }
            binding.neededProductsText.text = "Needed Products: $neededProducts"
            
            // Enable/disable navigation buttons
            binding.previousStepButton.isEnabled = state.currentStep > 0
            binding.nextStepButton.isEnabled = state.currentStep < state.totalSteps - 1
            
            android.util.Log.d("CookingModeFragment", "UI updated successfully")
        } catch (e: Exception) {
            android.util.Log.e("CookingModeFragment", "Error updating UI", e)
            e.printStackTrace()
        }
    }
    
    private fun updateTimerUI(remainingSeconds: Long) {
        if (!isAdded || _binding == null) {
            android.util.Log.w("CookingModeFragment", "Cannot update timer UI - fragment not ready")
            return
        }
        
        try {
            val hours = remainingSeconds / 3600
            val minutes = (remainingSeconds % 3600) / 60
            val seconds = remainingSeconds % 60
            
            val timeString = if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
            
            android.util.Log.d("CookingModeFragment", "Updating timer UI: $timeString ($remainingSeconds seconds)")
            binding.timerDisplayText.text = timeString
        } catch (e: Exception) {
            android.util.Log.e("CookingModeFragment", "Error updating timer UI", e)
            e.printStackTrace()
        }
    }
    
    private fun updateTimerControls(isRunning: Boolean, isPaused: Boolean, isFinished: Boolean, label: String?) {
        if (!isAdded || _binding == null) return
        
        try {
            if (isFinished) {
                // Timer finished - show stop alarm button
                binding.timerButton.text = "Change Timer"
                binding.timerControlsLayout.visibility = View.GONE
                binding.stopAlarmButton.visibility = View.VISIBLE
                binding.timerDisplayText.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
                if (label != null) {
                    binding.timerLabelText.text = "$label - Finished!"
                    binding.timerLabelText.visibility = View.VISIBLE
                } else {
                    binding.timerLabelText.text = "Timer Finished!"
                    binding.timerLabelText.visibility = View.VISIBLE
                }
            } else if (isRunning) {
                // Timer is running
                binding.timerButton.text = "Change Timer"
                binding.timerControlsLayout.visibility = View.VISIBLE
                binding.stopAlarmButton.visibility = View.GONE
                binding.pauseResumeTimerButton.text = if (isPaused) "Resume" else "Pause"
                binding.timerDisplayText.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                if (label != null) {
                    binding.timerLabelText.text = label
                    binding.timerLabelText.visibility = View.VISIBLE
                } else {
                    binding.timerLabelText.visibility = View.GONE
                }
            } else {
                // Timer not running
                binding.timerButton.text = "Set Timer"
                binding.timerControlsLayout.visibility = View.GONE
                binding.stopAlarmButton.visibility = View.GONE
                binding.timerDisplayText.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                binding.timerLabelText.visibility = View.GONE
            }
        } catch (e: Exception) {
            android.util.Log.e("CookingModeFragment", "Error updating timer controls", e)
        }
    }

    private fun checkPermissionAndStartVoiceRecognition() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startVoiceRecognition()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startVoiceRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(requireContext(), "Speech recognition not available", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak...")
        }

        speechRecognizer?.startListening(intent)
    }

    private fun stopVoiceRecognition() {
        speechRecognizer?.stopListening()
        isListening = false
        binding.microphoneButton.setIconResource(android.R.drawable.ic_btn_speak_now)
    }
    
    private fun speakText(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    private fun showTimerFinishedDialog(label: String?) {
        if (timerFinishedDialogShown || !isAdded) return
        
        timerFinishedDialogShown = true
        
        val dialog = TimerFinishedDialog.newInstance(label)
        dialog.onStopAlarm = {
            viewModel.stopTimer()
            timerFinishedDialogShown = false
        }
        dialog.onDismissCallback = {
            timerFinishedDialogShown = false
        }
        dialog.isCancelable = false // Don't allow dismissing by clicking outside
        dialog.show(childFragmentManager, "TimerFinishedDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
<<<<<<< Updated upstream
        speechRecognizer?.destroy()
=======
        try {
            requireContext().unregisterReceiver(timerUpdateReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        mlKitSpeechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
>>>>>>> Stashed changes
        _binding = null
    }
}

class CookingModeViewModelFactory(
    private val context: android.content.Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CookingModeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CookingModeViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

