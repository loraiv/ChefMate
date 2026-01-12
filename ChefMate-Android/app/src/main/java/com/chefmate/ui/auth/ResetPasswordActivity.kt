package com.chefmate.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chefmate.databinding.ActivityResetPasswordBinding
import com.chefmate.data.repository.AuthRepository
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPasswordBinding
    private lateinit var authRepository: AuthRepository
    private var resetToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityResetPasswordBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            authRepository = AuthRepository()
            
            resetToken = intent.getStringExtra("token")
            if (resetToken.isNullOrEmpty()) {
                val data = intent.data
                if (data != null) {
                    resetToken = data.getQueryParameter("token")
                }
            }
            
            if (resetToken.isNullOrEmpty()) {
                Toast.makeText(this, "Invalid reset link. Please request a new password reset.", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            
            setupClickListeners()
            setupPasswordToggles()
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupPasswordToggles() {
        binding.passwordInputLayout.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM
        binding.passwordInputLayout.setEndIconDrawable(com.google.android.material.R.drawable.design_ic_visibility_off)
        binding.confirmPasswordInputLayout.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM
        binding.confirmPasswordInputLayout.setEndIconDrawable(com.google.android.material.R.drawable.design_ic_visibility_off)
        
        var isPasswordVisible = false
        var isConfirmPasswordVisible = false
        
        binding.passwordInputLayout.setEndIconOnClickListener {
            val editText = binding.passwordEditText
            isPasswordVisible = !isPasswordVisible
            
            if (isPasswordVisible) {
                editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.passwordInputLayout.setEndIconDrawable(com.google.android.material.R.drawable.design_ic_visibility)
            } else {
                editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.passwordInputLayout.setEndIconDrawable(com.google.android.material.R.drawable.design_ic_visibility_off)
            }
            editText.setSelection(editText.text?.length ?: 0)
        }
        
        binding.confirmPasswordInputLayout.setEndIconOnClickListener {
            val editText = binding.confirmPasswordEditText
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            
            if (isConfirmPasswordVisible) {
                editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.confirmPasswordInputLayout.setEndIconDrawable(com.google.android.material.R.drawable.design_ic_visibility)
            } else {
                editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.confirmPasswordInputLayout.setEndIconDrawable(com.google.android.material.R.drawable.design_ic_visibility_off)
            }
            editText.setSelection(editText.text?.length ?: 0)
        }
    }

    private fun setupClickListeners() {
        binding.resetPasswordButton.setOnClickListener {
            val password = binding.passwordEditText.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

            if (password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            resetPassword(password)
        }

        binding.backToLoginTextView.setOnClickListener {
            finish()
        }
    }

    private fun resetPassword(newPassword: String) {
        val token = resetToken ?: return
        
        binding.resetPasswordButton.isEnabled = false
        binding.resetPasswordButton.text = "Resetting..."

        lifecycleScope.launch {
            try {
                Log.d("ResetPassword", "Attempting to reset password with token")
                authRepository.resetPassword(token, newPassword)
                    .onSuccess {
                        Log.d("ResetPassword", "Password reset successful")
                        Toast.makeText(
                            this@ResetPasswordActivity,
                            "Password reset successfully! You can now login with your new password.",
                            Toast.LENGTH_LONG
                        ).show()
                        
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }, 2000)
                    }
                    .onFailure { error ->
                        Log.e("ResetPassword", "Password reset failed: ${error.message}", error)
                        val errorMessage = error.message ?: "Unknown error occurred"
                        Toast.makeText(
                            this@ResetPasswordActivity,
                            "Error: $errorMessage",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.resetPasswordButton.isEnabled = true
                        binding.resetPasswordButton.text = "Reset Password"
                    }
            } catch (e: Exception) {
                Log.e("ResetPassword", "Exception in resetPassword", e)
                Toast.makeText(
                    this@ResetPasswordActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                binding.resetPasswordButton.isEnabled = true
                binding.resetPasswordButton.text = "Reset Password"
            }
        }
    }
}
