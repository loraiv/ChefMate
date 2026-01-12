package com.chefmate.ui.cooking

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import com.chefmate.R
import com.chefmate.data.api.models.RecipeResponse
import com.chefmate.databinding.FragmentCookingModeBinding
import kotlinx.coroutines.launch
import java.util.Locale

class CookingModeFragment : Fragment() {

    private var _binding: FragmentCookingModeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CookingModeViewModel by viewModels {
        CookingModeViewModelFactory(requireContext())
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceRecognition()
        } else {
            Toast.makeText(requireContext(), "Microphone permission is required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCookingModeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recipeId = arguments?.getLong("recipeId") ?: -1L
        if (recipeId == -1L) {
            Toast.makeText(requireContext(), "Error: Recipe not found", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
            return
        }

        // Load recipe and start session
        loadRecipeAndStart(recipeId)
        setupSpeechRecognizer()
        setupClickListeners()
        setupObservers()
    }
    
    private fun loadRecipeAndStart(recipeId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Load recipe from repository
            val tokenManager = com.chefmate.utils.TokenManager(requireContext())
            val apiService = com.chefmate.di.AppModule.provideApiService()
            val recipeRepository = com.chefmate.data.repository.RecipeRepository(apiService, tokenManager)
            
            recipeRepository.getRecipeById(recipeId)
                .onSuccess { recipe ->
                    viewModel.startCookingSession(recipe)
                    updateUI()
                }
                .onFailure { exception ->
                    Toast.makeText(requireContext(), "Error loading recipe: ${exception.message}", Toast.LENGTH_SHORT).show()
                    requireActivity().onBackPressed()
                }
        }
    }

    private fun setupSpeechRecognizer() {
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
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessionState.collect { state ->
                updateUI()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.aiResponse.collect { response ->
                if (response != null) {
                    binding.aiResponseCard.visibility = View.VISIBLE
                    binding.aiResponseText.text = response
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
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun updateUI() {
        val state = viewModel.sessionState.value
        val recipe = state.recipe ?: return

        // Recipe title
        binding.recipeTitle.text = recipe.title

        // Current step
        binding.currentStepText.text = "Step: ${state.currentStep + 1}/${state.totalSteps}"

        // Timer
        val minutes = state.elapsedTimeSeconds / 60
        val seconds = state.elapsedTimeSeconds % 60
        binding.timerText.text = String.format("%02d:%02d", minutes, seconds)

        // Stove setting
        binding.stoveSettingText.text = state.stoveSetting ?: "Not set"

        // Current action
        binding.currentActionText.text = "Action: ${state.currentAction ?: "No action"}"

        // Needed products (extract from current step or use recipe ingredients)
        val neededProducts = if (state.currentStep < recipe.ingredients.size) {
            recipe.ingredients[state.currentStep]
        } else {
            recipe.ingredients.joinToString(", ")
        }
        binding.neededProductsText.text = "Needed Products: $neededProducts"
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

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer?.destroy()
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

