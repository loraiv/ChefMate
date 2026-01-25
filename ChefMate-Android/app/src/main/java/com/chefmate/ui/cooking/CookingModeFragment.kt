package com.chefmate.ui.cooking

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
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
import com.chefmate.utils.MlKitSpeechRecognizer
import kotlinx.coroutines.launch
import java.util.Locale

class CookingModeFragment : Fragment() {

    private var _binding: FragmentCookingModeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CookingModeViewModel by viewModels {
        CookingModeViewModelFactory(requireActivity().applicationContext)
    }

    private var mlKitSpeechRecognizer: MlKitSpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var autoSpeakEnabled = true // Default to enabled
    private var timerFinishedDialogShown = false
    private var previousTimerState: CookingSessionState? = null
    private var isListening = false
    private var currentTtsText: String? = null
    private var isTtsPaused = false
    private var ttsPausedPosition: Int = 0
    private var ttsUtteranceId: String = "tts_utterance"
    private var ttsStartTime: Long = 0
    private var ttsTotalDuration: Long = 0
    private var ttsElapsedTime: Long = 0
    private val ttsWordsPerMinute = 150 // Average speaking rate
    private var ttsSentences: List<String> = emptyList()
    private var ttsCurrentSentenceIndex: Int = 0
    
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
                    Toast.makeText(requireContext(), "Recipe not found. Please try again.", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                return
            }

            // Setup toolbar
            setupToolbar()
            
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
                    Toast.makeText(requireContext(), "Unable to start cooking mode. Please try again.", Toast.LENGTH_SHORT).show()
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
    
    private fun setupToolbar() {
        val toolbar = binding.toolbar
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.let { activity ->
            activity.setSupportActionBar(toolbar)
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            activity.supportActionBar?.setDisplayShowHomeEnabled(true)
            activity.supportActionBar?.title = ""
        }
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
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
                    
                    // Setup UtteranceProgressListener to track TTS progress
                    textToSpeech?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            android.util.Log.d("CookingModeFragment", "TTS started: $utteranceId")
                        }
                        
                        override fun onDone(utteranceId: String?) {
                            android.util.Log.d("CookingModeFragment", "TTS done: $utteranceId")
                            if (isAdded && _binding != null) {
                                requireActivity().runOnUiThread {
                                    // Move to next sentence if available
                                    if (utteranceId?.startsWith("sentence_") == true) {
                                        val sentenceIndex = utteranceId.removePrefix("sentence_").toIntOrNull() ?: -1
                                        if (sentenceIndex >= 0 && sentenceIndex < ttsSentences.size - 1) {
                                            // Continue with next sentence
                                            ttsCurrentSentenceIndex = sentenceIndex + 1
                                            if (!isTtsPaused) {
                                                speakNextSentence()
                                            }
                                        } else {
                                            // All sentences done
                                            isTtsPaused = false
                                            ttsPausedPosition = 0
                                            ttsElapsedTime = 0
                                            ttsStartTime = 0
                                            ttsTotalDuration = 0
                                            ttsCurrentSentenceIndex = 0
                                            updateTtsButtonStates()
                                        }
                                    } else {
                                        // Old style - single utterance
                                        isTtsPaused = false
                                        ttsPausedPosition = 0
                                        ttsElapsedTime = 0
                                        ttsStartTime = 0
                                        ttsTotalDuration = 0
                                        updateTtsButtonStates()
                                    }
                                }
                            }
                        }
                        
                        override fun onError(utteranceId: String?) {
                            android.util.Log.e("CookingModeFragment", "TTS error: $utteranceId")
                            if (isAdded && _binding != null) {
                                requireActivity().runOnUiThread {
                                    isTtsPaused = false
                                    ttsPausedPosition = 0
                                    ttsElapsedTime = 0
                                    updateTtsButtonStates()
                                }
                            }
                        }
                    })
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
                            Toast.makeText(requireContext(), "Unable to load recipe. Please try again.", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                    }
            } catch (e: Exception) {
                android.util.Log.e("CookingModeFragment", "Error loading recipe", e)
                e.printStackTrace()
                if (isAdded && _binding != null) {
                    binding.loadingIndicator.visibility = View.GONE
                    Toast.makeText(requireContext(), "Unable to load recipe. Please try again.", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun setupSpeechRecognizer() {
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
            isListening = false
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
            isListening = false
        }
        
        mlKitSpeechRecognizer?.onListeningStateChanged = { listening ->
            isListening = listening
            if (listening) {
                binding.microphoneButton.setIconResource(R.drawable.ic_pause)
            } else {
                binding.microphoneButton.setIconResource(R.drawable.ic_mic)
            }
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
        
        // TTS Control Buttons
        binding.pauseTtsButton.setOnClickListener {
            android.util.Log.d("CookingModeFragment", "Pause TTS button clicked")
            pauseTts()
        }
        
        binding.playTtsButton.setOnClickListener {
            android.util.Log.d("CookingModeFragment", "Play TTS button clicked, isPaused: $isTtsPaused")
            if (isTtsPaused) {
                resumeTts()
            } else {
                startTts()
            }
        }
        
        binding.restartTtsButton.setOnClickListener {
            android.util.Log.d("CookingModeFragment", "Restart TTS button clicked")
            restartTts()
        }
        
        // Make sure buttons are enabled and clickable
        binding.pauseTtsButton.isEnabled = true
        binding.playTtsButton.isEnabled = true
        binding.restartTtsButton.isEnabled = true
    }
    
    private fun showTimerDialog() {
        if (!isAdded || childFragmentManager == null) return
        
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
                    currentTtsText = response
                    updateTtsButtonStates()
                    // Automatically speak AI response only if enabled
                    if (autoSpeakEnabled) {
                        speakText(response)
                    }
                } else {
                    binding.aiResponseCard.visibility = View.GONE
                    currentTtsText = null
                    updateTtsButtonStates()
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
        if (!isAdded) return
        
        mlKitSpeechRecognizer?.startListening("en-US")
    }

    private fun stopVoiceRecognition() {
        mlKitSpeechRecognizer?.stopListening()
        isListening = false
        binding.microphoneButton.setIconResource(R.drawable.ic_mic)
    }
    
    private fun speakText(text: String) {
        currentTtsText = text
        isTtsPaused = false
        ttsPausedPosition = 0
        ttsElapsedTime = 0
        ttsCurrentSentenceIndex = 0
        
        // Split text into sentences for better pause/resume control
        ttsSentences = splitIntoSentences(text)
        
        // Start speaking from first sentence
        speakNextSentence()
        updateTtsButtonStates()
    }
    
    private fun splitIntoSentences(text: String): List<String> {
        // Split by sentence endings (. ! ?) followed by whitespace
        val sentences = mutableListOf<String>()
        val pattern = Regex("([^.!?]+[.!?]+\\s*)")
        val matches = pattern.findAll(text)
        
        matches.forEach { match ->
            val sentence = match.value.trim()
            if (sentence.isNotEmpty()) {
                sentences.add(sentence)
            }
        }
        
        // If no sentences found (no punctuation), treat whole text as one sentence
        if (sentences.isEmpty()) {
            sentences.add(text.trim())
        }
        
        return sentences
    }
    
    private fun speakNextSentence() {
        if (ttsCurrentSentenceIndex < ttsSentences.size) {
            val sentence = ttsSentences[ttsCurrentSentenceIndex]
            val utteranceId = "sentence_$ttsCurrentSentenceIndex"
            
            val params = android.os.Bundle()
            params.putString(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            textToSpeech?.speak(sentence, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            
            ttsStartTime = System.currentTimeMillis()
            android.util.Log.d("CookingModeFragment", "Speaking sentence $ttsCurrentSentenceIndex: $sentence")
        }
    }
    
    private fun pauseTts() {
        textToSpeech?.stop()
        isTtsPaused = true
        // Keep current sentence index - we'll resume from the same sentence
        updateTtsButtonStates()
    }
    
    private fun resumeTts() {
        // Resume from current sentence index (exact position!)
        if (ttsCurrentSentenceIndex < ttsSentences.size) {
            isTtsPaused = false
            speakNextSentence()
            updateTtsButtonStates()
        } else {
            // Already finished
            isTtsPaused = false
            ttsCurrentSentenceIndex = 0
            updateTtsButtonStates()
        }
    }
    
    private fun restartTts() {
        textToSpeech?.stop()
        isTtsPaused = false
        ttsPausedPosition = 0
        ttsElapsedTime = 0
        ttsStartTime = 0
        ttsTotalDuration = 0
        ttsCurrentSentenceIndex = 0
        // Restart from first sentence
        speakNextSentence()
        updateTtsButtonStates()
    }
    
    private fun startTts() {
        // Start from first sentence
        ttsCurrentSentenceIndex = 0
        isTtsPaused = false
        ttsPausedPosition = 0
        speakNextSentence()
        updateTtsButtonStates()
    }
    
    private fun updateTtsButtonStates() {
        if (!isAdded || _binding == null) return
        
        // Show/hide buttons based on TTS state
        val hasText = currentTtsText != null && currentTtsText!!.isNotEmpty()
        
        if (hasText) {
            binding.pauseTtsButton.visibility = if (!isTtsPaused) View.VISIBLE else View.GONE
            binding.playTtsButton.visibility = if (isTtsPaused) View.VISIBLE else View.GONE
            binding.restartTtsButton.visibility = View.VISIBLE
            
            // Enable/disable buttons
            binding.pauseTtsButton.isEnabled = !isTtsPaused
            binding.playTtsButton.isEnabled = isTtsPaused
            binding.restartTtsButton.isEnabled = true
        } else {
            binding.pauseTtsButton.visibility = View.GONE
            binding.playTtsButton.visibility = View.GONE
            binding.restartTtsButton.visibility = View.GONE
        }
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
        try {
            requireContext().unregisterReceiver(timerUpdateReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        mlKitSpeechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
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
