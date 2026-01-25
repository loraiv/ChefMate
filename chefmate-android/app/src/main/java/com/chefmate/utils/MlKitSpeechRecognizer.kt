package com.chefmate.utils

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.languageid.LanguageIdentifier
import java.util.Locale

/**
 * Enhanced Speech Recognition helper using Android SpeechRecognizer
 * with ML Kit Language Identification for better language detection
 * 
 * Note: ML Kit doesn't have a direct Speech Recognition API.
 * This wrapper improves the existing Android SpeechRecognizer with ML Kit features.
 */
class MlKitSpeechRecognizer(private val context: Context) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    // ML Kit language identifier client
    private var languageIdentifier: LanguageIdentifier? = null
    private var isListening = false
    
    // Callbacks
    var onResult: ((String) -> Unit)? = null
    var onPartialResult: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onListeningStateChanged: ((Boolean) -> Unit)? = null
    
    companion object {
        private const val TAG = "MlKitSpeechRecognizer"
        
        // Supported languages
        val SUPPORTED_LANGUAGES = listOf(
            "bg-BG", // Bulgarian
            "en-US", // English US
            "en-GB", // English UK
            "de-DE", // German
            "fr-FR", // French
            "es-ES", // Spanish
            "it-IT", // Italian
            "ru-RU", // Russian
        )
    }
    
    init {
        initialize()
    }
    
    private fun initialize() {
        try {
            // Initialize ML Kit Language Identifier (for detecting language from recognized text)
            languageIdentifier = LanguageIdentification.getClient(
                LanguageIdentificationOptions.Builder()
                    .setConfidenceThreshold(0.5f)
                    .build()
            )
            
            // Initialize Android SpeechRecognizer
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                setupRecognitionListener()
                Log.d(TAG, "Speech Recognition initialized with ML Kit Language ID support")
            } else {
                Log.e(TAG, "Speech recognition is not available on this device")
                onError?.invoke("Speech recognition is not available on this device")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Speech Recognition", e)
            onError?.invoke("Failed to initialize speech recognition: ${e.message}")
        }
    }
    
    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                isListening = true
                onListeningStateChanged?.invoke(true)
            }
            
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changed (can be used for visual feedback)
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer received
            }
            
            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
                isListening = false
                onListeningStateChanged?.invoke(false)
            }
            
            override fun onError(error: Int) {
                isListening = false
                onListeningStateChanged?.invoke(false)
                
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                    else -> "Unknown error: $error"
                }
                
                Log.e(TAG, "Speech recognition error: $errorMessage")
                onError?.invoke(errorMessage)
            }
            
            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    Log.d(TAG, "Final result: $recognizedText")
                    
                    // Optionally detect language using ML Kit
                    detectLanguageIfNeeded(recognizedText) {
                        onResult?.invoke(recognizedText)
                    }
                } else {
                    onError?.invoke("No recognition results")
                }
            }
            
            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val partialText = matches[0]
                    Log.d(TAG, "Partial result: $partialText")
                    onPartialResult?.invoke(partialText)
                }
            }
            
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {
                // Additional events
            }
        })
    }
    
    /**
     * Start listening for speech
     * @param languageCode Language code (e.g., "bg-BG", "en-US"). Defaults to Bulgarian
     */
    fun startListening(languageCode: String = "bg-BG") {
        if (isListening) {
            Log.w(TAG, "Already listening")
            return
        }
        
        if (speechRecognizer == null) {
            Log.e(TAG, "Speech recognizer not initialized")
            onError?.invoke("Speech recognizer not available")
            return
        }
        
        try {
            val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak...")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            
            speechRecognizer?.startListening(intent)
            Log.d(TAG, "Started listening for language: $languageCode")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            isListening = false
            onListeningStateChanged?.invoke(false)
            onError?.invoke("Failed to start listening: ${e.message}")
        }
    }
    
    /**
     * Stop listening for speech
     */
    fun stopListening() {
        if (!isListening) {
            return
        }
        
        try {
            speechRecognizer?.stopListening()
            isListening = false
            onListeningStateChanged?.invoke(false)
            Log.d(TAG, "Stopped listening")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop listening", e)
        }
    }
    
    /**
     * Check if currently listening
     */
    fun isCurrentlyListening(): Boolean = isListening
    
    /**
     * Detect language from recognized text using ML Kit (optional feature)
     */
    private fun detectLanguageIfNeeded(text: String, onComplete: () -> Unit) {
        if (text.length < 3) {
            // Too short for language detection
            onComplete()
            return
        }
        
        languageIdentifier?.identifyLanguage(text)
            ?.addOnSuccessListener { languageCode ->
                Log.d(TAG, "ML Kit detected language: $languageCode from text: $text")
                // Could use this to auto-adjust language for next recognition
                onComplete()
            }
            ?.addOnFailureListener { e ->
                Log.w(TAG, "Language detection failed (non-critical)", e)
                onComplete()
            }
    }
    
    /**
     * Detect language from text (public API)
     */
    fun detectLanguage(text: String, onDetected: (String?) -> Unit) {
        if (text.length < 3) {
            onDetected(null)
            return
        }
        
        languageIdentifier?.identifyLanguage(text)
            ?.addOnSuccessListener { languageCode ->
                Log.d(TAG, "Detected language: $languageCode")
                onDetected(languageCode)
            }
            ?.addOnFailureListener { e ->
                Log.e(TAG, "Language detection failed", e)
                onDetected(null)
            }
    }
    
    /**
     * Check if speech recognition is available on this device
     */
    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        try {
            stopListening()
            speechRecognizer?.destroy()
            languageIdentifier?.close()
            speechRecognizer = null
            languageIdentifier = null
            Log.d(TAG, "Speech Recognition destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}
