package com.chefmate.ui.auth

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chefmate.databinding.ActivityForgotPasswordBinding
import com.chefmate.data.repository.AuthRepository
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            authRepository = AuthRepository()
            
            setupClickListeners()
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to load the screen. Please try again.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupClickListeners() {
        binding.sendResetButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter an email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendPasswordReset(email)
        }

        binding.backToLoginTextView.setOnClickListener {
            finish()
        }
    }

    private fun sendPasswordReset(email: String) {
        Log.d("ForgotPassword", "sendPasswordReset called with email: $email")
        binding.sendResetButton.isEnabled = false
        binding.sendResetButton.text = "Sending..."

        lifecycleScope.launch {
            try {
                Log.d("ForgotPassword", "Attempting to send password reset for email: $email")
                authRepository.forgotPassword(email)
                    .onSuccess { responseMap ->
                        Log.d("ForgotPassword", "Password reset request successful. Response: $responseMap")
                        val token = responseMap["token"]
                        val message = responseMap["message"]
                        
                        if (token != null) {
                            // Email service is not configured, show token dialog
                            val dialogMessage = "Email service is not configured.\n\n" +
                                    "Your reset token:\n$token\n\n" +
                                    "Copy this token and use it in the Reset Password screen."
                            android.app.AlertDialog.Builder(this@ForgotPasswordActivity)
                                .setTitle("Password Reset Token")
                                .setMessage(dialogMessage)
                                .setPositiveButton("Copy Token") { _, _ ->
                                    val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Reset Token", token)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(this@ForgotPasswordActivity, "Token copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                                .setNeutralButton("Go to Reset") { _, _ ->
                                    val intent = android.content.Intent(this@ForgotPasswordActivity, com.chefmate.ui.auth.ResetPasswordActivity::class.java)
                                    intent.putExtra("token", token)
                                    startActivity(intent)
                                    finish()
                                }
                                .setNegativeButton("OK", null)
                                .show()
                        } else {
                            // Email was sent successfully
                            val successMessage = message ?: "Password reset instructions have been sent to your email address.\n\nPlease check your email and click the link to reset your password."
                            android.app.AlertDialog.Builder(this@ForgotPasswordActivity)
                                .setTitle("Email Sent")
                                .setMessage(successMessage)
                                .setPositiveButton("OK") { _, _ ->
                                    finish()
                                }
                                .show()
                        }
                        binding.sendResetButton.isEnabled = true
                        binding.sendResetButton.text = "Send Instructions"
                    }
                    .onFailure { error ->
                        Log.e("ForgotPassword", "Password reset failed: ${error.message}", error)
                        val userFriendlyMessage = when {
                            error.message?.contains("network", ignoreCase = true) == true -> 
                                "Network error. Please check your internet connection and try again."
                            error.message?.contains("timeout", ignoreCase = true) == true -> 
                                "Request timed out. Please try again."
                            else -> 
                                "Unable to send password reset email. Please try again later."
                        }
                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            userFriendlyMessage,
                            Toast.LENGTH_LONG
                        ).show()
                        binding.sendResetButton.isEnabled = true
                        binding.sendResetButton.text = "Send Instructions"
                    }
            } catch (e: Exception) {
                Log.e("ForgotPassword", "Exception in sendPasswordReset", e)
                Toast.makeText(
                    this@ForgotPasswordActivity,
                    "An unexpected error occurred. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
                binding.sendResetButton.isEnabled = true
                binding.sendResetButton.text = "Send Instructions"
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            Pattern.CASE_INSENSITIVE
        )
        return emailPattern.matcher(email).matches()
    }
}

