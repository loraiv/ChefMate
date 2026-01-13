package com.chefmate.ui.cooking

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.chefmate.utils.MlKitSpeechRecognizer
import kotlinx.coroutines.launch
import java.util.Locale

class CookingModeFragment : Fragment() {

    private var _binding: FragmentCookingModeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CookingModeViewModel by viewModels {
        CookingModeViewModelFactory(requireContext())
    }

    private var mlKitSpeechRecognizer: MlKitSpeechRecognizer? = null

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
        mlKitSpeechRecognizer = MlKitSpeechRecognizer(requireContext())
        
        if (!mlKitSpeechRecognizer?.isAvailable()!!) {
            Toast.makeText(requireContext(), "Speech recognition is not available on this device", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Setup callbacks
        mlKitSpeechRecognizer?.onResult = { recognizedText ->
            viewModel.sendVoiceMessage(recognizedText)
            binding.microphoneButton.setIconResource(R.drawable.ic_mic)
        }
        
        mlKitSpeechRecognizer?.onError = { errorMessage ->
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            binding.microphoneButton.setIconResource(R.drawable.ic_mic)
        }
        
        mlKitSpeechRecognizer?.onListeningStateChanged = { isListening ->
            if (isListening) {
                binding.microphoneButton.setIconResource(R.drawable.ic_pause)
            } else {
                binding.microphoneButton.setIconResource(R.drawable.ic_mic)
            }
        }
    }

    private fun setupClickListeners() {
        binding.microphoneButton.setOnClickListener {
            if (mlKitSpeechRecognizer?.isCurrentlyListening() == true) {
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
        if (mlKitSpeechRecognizer?.isAvailable() != true) {
            Toast.makeText(requireContext(), "Speech recognition is not available on this device", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Start listening with Bulgarian language (can be changed based on user preference)
        mlKitSpeechRecognizer?.startListening("bg-BG")
    }

    private fun stopVoiceRecognition() {
        mlKitSpeechRecognizer?.stopListening()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mlKitSpeechRecognizer?.destroy()
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

