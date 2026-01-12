package com.chefmate.ui.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isListening = false
    private var shouldSpeakResponse = false // Only speak if user used voice input
    private lateinit var tokenManager: TokenManager
    private var autoSpeakEnabled = false
    private var ttsLanguage = Locale.US // Default to English for better quality

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceRecognition()
        } else {
            Toast.makeText(requireContext(), "Microphone permission is required for voice input", Toast.LENGTH_LONG).show()
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
        chatAdapter = ChatAdapter(emptyList())
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.chatRecyclerView.adapter = chatAdapter
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
        // Find the bottom navigation view from MainActivity and use its height
        fun applyBottomMargin() {
            val activity = requireActivity()
            val bottomNavView = activity.findViewById<View>(com.chefmate.R.id.bottomNavigationView)
            
            val rootInsets = ViewCompat.getRootWindowInsets(binding.root)
            val imeInsets = rootInsets?.getInsets(WindowInsetsCompat.Type.ime())
            val systemBarsInsets = rootInsets?.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // If keyboard is visible, position above keyboard, otherwise above bottom navigation
            val bottomMargin = if (imeInsets != null && imeInsets.bottom > 0) {
                // Keyboard is visible - position above keyboard
                imeInsets.bottom
            } else {
                // Keyboard is hidden - position above bottom navigation bar
                val navigationBarHeight = if (bottomNavView != null && bottomNavView.visibility == View.VISIBLE && bottomNavView.height > 0) {
                    bottomNavView.height
                } else {
                    systemBarsInsets?.bottom ?: 0
                }
                navigationBarHeight
            }
            
            if (bottomMargin >= 0) {
                val layoutParams = binding.controlsContainer.layoutParams as ConstraintLayout.LayoutParams
                if (layoutParams.bottomMargin != bottomMargin) {
                    layoutParams.bottomMargin = bottomMargin
                    binding.controlsContainer.layoutParams = layoutParams
                }
            }
        }
        
        // Apply after view is laid out
        binding.root.post {
            applyBottomMargin()
        }
        
        // Listen for insets changes (keyboard show/hide)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            applyBottomMargin()
            insets
        }
        
        // Also listen for layout changes
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                applyBottomMargin()
            }
        })
    }

    private fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    binding.voiceButton.setIconResource(R.drawable.ic_pause)
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                    binding.voiceButton.setIconResource(R.drawable.ic_mic)
                }

                override fun onError(error: Int) {
                    isListening = false
                    binding.voiceButton.setIconResource(R.drawable.ic_mic)
                    when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> Toast.makeText(requireContext(), "Audio error", Toast.LENGTH_SHORT).show()
                        SpeechRecognizer.ERROR_CLIENT -> {}
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> Toast.makeText(requireContext(), "No microphone permission", Toast.LENGTH_SHORT).show()
                        SpeechRecognizer.ERROR_NETWORK -> Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> Toast.makeText(requireContext(), "Network timeout", Toast.LENGTH_SHORT).show()
                        SpeechRecognizer.ERROR_NO_MATCH -> Toast.makeText(requireContext(), "Speech not recognized", Toast.LENGTH_SHORT).show()
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> Toast.makeText(requireContext(), "Recognizer is busy", Toast.LENGTH_SHORT).show()
                        SpeechRecognizer.ERROR_SERVER -> Toast.makeText(requireContext(), "Server error", Toast.LENGTH_SHORT).show()
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> Toast.makeText(requireContext(), "No speech detected", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val spokenText = matches[0]
                        binding.messageEditText.setText(spokenText)
                        // Auto-send voice input and enable speaking for response
                        shouldSpeakResponse = true
                        sendMessage(spokenText)
                    }
                    isListening = false
                    binding.voiceButton.setIconResource(R.drawable.ic_mic)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        binding.messageEditText.setText(matches[0])
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
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
            if (isListening) {
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
            viewModel.chatMessages.collect { messages ->
                chatAdapter = ChatAdapter(messages)
                binding.chatRecyclerView.adapter = chatAdapter
                binding.chatRecyclerView.scrollToPosition(messages.size - 1)
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
                    // Speak if auto-speak is enabled OR if user used voice input
                    if (autoSpeakEnabled || shouldSpeakResponse) {
                        speakText(messages.last().message)
                        shouldSpeakResponse = false // Reset after speaking
                    }
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
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(requireContext(), "Speech recognition is not available on this device", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bg-BG")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak...")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.startListening(intent)
    }

    private fun stopVoiceRecognition() {
        speechRecognizer?.stopListening()
        isListening = false
        binding.voiceButton.setIconResource(android.R.drawable.ic_btn_speak_now)
    }

    private fun speakText(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer?.destroy()
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
