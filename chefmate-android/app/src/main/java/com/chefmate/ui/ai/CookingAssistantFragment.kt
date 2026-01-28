package com.chefmate.ui.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.constraintlayout.widget.ConstraintLayout
import com.chefmate.R
import com.chefmate.databinding.FragmentCookingAssistantBinding
import com.chefmate.ui.ai.adapter.ChatAdapter
import com.chefmate.ui.ai.viewmodel.AiViewModel
import com.chefmate.utils.MlKitSpeechRecognizer
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.launch
import java.util.Locale

class CookingAssistantFragment : Fragment() {

    private var _binding: FragmentCookingAssistantBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AiViewModel by viewModels {
        AiViewModelFactory(requireContext())
    }

    private lateinit var chatAdapter: ChatAdapter
    private var mlKitSpeechRecognizer: MlKitSpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var shouldSpeakResponse = false // Only speak if user used voice input
    private lateinit var tokenManager: TokenManager
    private var autoSpeakEnabled = false
    private var ttsLanguage = Locale.US

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceRecognition()
        } else {
            Toast.makeText(requireContext(), "Microphone permission is required to use voice input", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCookingAssistantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())
        autoSpeakEnabled = tokenManager.isAutoSpeakEnabled()

        setupRecyclerView()
        setupTextToSpeech()
        setupSpeechRecognizer()
        setupAutoSpeakToggle()
        setupClickListeners()
        setupObservers()
        setupProgressBar()
        setupBottomNavigationPadding()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.chatRecyclerView.adapter = chatAdapter
        binding.chatRecyclerView.setHasFixedSize(false)
    }

    private fun setupTextToSpeech() {
        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Always set to English
                val result = textToSpeech?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to default language if English is not supported
                    textToSpeech?.setLanguage(Locale.getDefault())
                    ttsLanguage = Locale.getDefault()
                } else {
                    ttsLanguage = Locale.US
                }
            }
        }
    }

    private fun setupAutoSpeakToggle() {
        binding.autoSpeakSwitch.isChecked = autoSpeakEnabled
        binding.autoSpeakSwitch.setOnCheckedChangeListener { _, isChecked ->
            autoSpeakEnabled = isChecked
            tokenManager.saveAutoSpeakEnabled(isChecked)
            // Reset shouldSpeakResponse when toggle is changed
            shouldSpeakResponse = false
        }
    }

    private fun setupProgressBar() {
        // Set progress bar color programmatically
        binding.loadingIndicator.indeterminateTintList = ContextCompat.getColorStateList(
            requireContext(),
            com.chefmate.R.color.primary
        )
    }

    private fun setupBottomNavigationPadding() {
        // Set initial bottom margin for bottom navigation bar
        binding.root.post {
            val activity = requireActivity()
            val bottomNavView = activity.findViewById<View>(com.chefmate.R.id.bottomNavigationView)
            val initialMargin = if (bottomNavView != null && bottomNavView.visibility == View.VISIBLE && bottomNavView.height > 0) {
                bottomNavView.height
            } else {
                (56 * resources.displayMetrics.density).toInt() // Default bottom navigation height
            }
            
            val layoutParams = binding.controlsContainer.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.bottomMargin = initialMargin
            binding.controlsContainer.layoutParams = layoutParams
        }
        
        // Listen for keyboard show/hide and adjust bottom margin accordingly
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            val bottomMargin = if (imeInsets.bottom > 0) {
                // Keyboard is visible - position above keyboard
                imeInsets.bottom
            } else {
                // Keyboard is hidden - position above bottom navigation bar
                val activity = requireActivity()
                val bottomNavView = activity.findViewById<View>(com.chefmate.R.id.bottomNavigationView)
                if (bottomNavView != null && bottomNavView.visibility == View.VISIBLE && bottomNavView.height > 0) {
                    bottomNavView.height
                } else {
                    systemBarsInsets.bottom.takeIf { it > 0 } ?: (56 * resources.displayMetrics.density).toInt()
                }
            }
            
            val layoutParams = binding.controlsContainer.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.bottomMargin = bottomMargin
            binding.controlsContainer.layoutParams = layoutParams
            
            insets
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
            binding.messageEditText.setText(recognizedText)
            // Auto-send voice input and enable speaking for response only if auto-speak is enabled
            shouldSpeakResponse = autoSpeakEnabled
            sendMessage(recognizedText)
            binding.voiceButton.setIconResource(R.drawable.ic_mic)
        }
        
        mlKitSpeechRecognizer?.onPartialResult = { partialText ->
            binding.messageEditText.setText(partialText)
        }
        
        mlKitSpeechRecognizer?.onError = { errorMessage ->
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            binding.voiceButton.setIconResource(R.drawable.ic_mic)
        }
        
        mlKitSpeechRecognizer?.onListeningStateChanged = { isListening ->
            if (isListening) {
                binding.voiceButton.setIconResource(R.drawable.ic_pause)
            } else {
                binding.voiceButton.setIconResource(R.drawable.ic_mic)
            }
        }
    }

    private fun setupClickListeners() {
        binding.sendButton.setOnClickListener {
            val message = binding.messageEditText.text?.toString()?.trim()
            if (!message.isNullOrEmpty()) {
                sendMessage(message)
                binding.messageEditText.text?.clear()
            }
        }

        binding.voiceButton.setOnClickListener {
            if (mlKitSpeechRecognizer?.isCurrentlyListening() == true) {
                stopVoiceRecognition()
            } else {
                checkPermissionAndStartVoiceRecognition()
            }
        }

        binding.messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.sendButton.isEnabled = !s.isNullOrBlank()
            }
        })
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            var previousSize = 0
            viewModel.chatMessages.collect { messages ->
                val wasEmpty = previousSize == 0
                val currentSize = chatAdapter.itemCount
                chatAdapter.submitList(messages) {
                    // Scroll to bottom after adapter updates, but only if new message was added
                    if (messages.isNotEmpty() && (messages.size > currentSize || wasEmpty)) {
                        binding.chatRecyclerView.postDelayed({
                            val lastPosition = messages.size - 1
                            if (lastPosition >= 0 && lastPosition < chatAdapter.itemCount) {
                                binding.chatRecyclerView.smoothScrollToPosition(lastPosition)
                            }
                        }, 100)
                    }
                }
                previousSize = messages.size
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.sendButton.isEnabled = !isLoading
                binding.voiceButton.isEnabled = !isLoading
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

        // Auto-speak AI responses
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.chatMessages.collect { messages ->
                if (messages.isNotEmpty() && !messages.last().isUser) {
                    // Speak only if auto-speak is enabled
                    if (autoSpeakEnabled) {
                        speakText(messages.last().message)
                    }
                    // Reset shouldSpeakResponse after processing response
                    shouldSpeakResponse = false
                }
            }
        }
    }

    private fun sendMessage(message: String) {
        viewModel.sendMessage(message)
        // shouldSpeakResponse is set to true only when voice input is used
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

    private fun speakText(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mlKitSpeechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        _binding = null
    }
}

class AiViewModelFactory(
    private val context: android.content.Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AiViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AiViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
