package com.chefmate.ui.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.chefmate.R
import com.chefmate.databinding.ActivityRegisterBinding
import com.chefmate.data.repository.AuthRepository
import com.chefmate.ui.main.MainActivity
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        authRepository = AuthRepository()

        setupClickListeners()
        setupRealTimeValidation()
        setupPasswordToggles()
    }

    private fun setupClickListeners() {
        binding.registerButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (username.length < 3) {
                Toast.makeText(this, "Username must be at least 3 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            register(username, email, password)
        }

        binding.loginTextView.setOnClickListener {
            finish()
        }
    }

    private fun setupRealTimeValidation() {
        binding.usernameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val username = s?.toString()?.trim() ?: ""
                if (username.isNotEmpty() && username.length < 3) {
                    showError(binding.usernameErrorText, "Username must be at least 3 characters")
                } else {
                    hideError(binding.usernameErrorText)
                }
            }
        })

        binding.emailEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val email = s?.toString()?.trim() ?: ""
                if (email.isNotEmpty() && !isValidEmail(email)) {
                    showError(binding.emailErrorText, "Please enter a valid email address")
                } else {
                    hideError(binding.emailErrorText)
                }
            }
        })

        binding.passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s?.toString() ?: ""
                updatePasswordStrength(password)
            }
        })

        binding.confirmPasswordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = binding.passwordEditText.text?.toString() ?: ""
                val confirmPassword = s?.toString() ?: ""
                if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                    showError(binding.confirmPasswordErrorText, "Passwords do not match")
                } else {
                    hideError(binding.confirmPasswordErrorText)
                }
            }
        })
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

    private fun updatePasswordStrength(password: String) {
        if (password.isEmpty()) {
            binding.passwordStrengthBar.visibility = android.view.View.GONE
            binding.passwordStrengthText.visibility = android.view.View.GONE
            return
        }

        binding.passwordStrengthBar.visibility = android.view.View.VISIBLE
        binding.passwordStrengthText.visibility = android.view.View.VISIBLE

        val strength = calculatePasswordStrength(password)
        binding.passwordStrengthBar.progress = strength.percentage

        val color = when (strength.level) {
            PasswordStrengthLevel.WEAK -> ContextCompat.getColor(this, R.color.password_weak)
            PasswordStrengthLevel.MEDIUM -> ContextCompat.getColor(this, R.color.password_medium)
            PasswordStrengthLevel.STRONG -> ContextCompat.getColor(this, R.color.password_strong)
            PasswordStrengthLevel.VERY_STRONG -> ContextCompat.getColor(this, R.color.password_very_strong)
        }

        binding.passwordStrengthBar.progressTintList = android.content.res.ColorStateList.valueOf(color)
        binding.passwordStrengthText.text = strength.text
        binding.passwordStrengthText.setTextColor(color)
    }

    private fun calculatePasswordStrength(password: String): PasswordStrength {
        var score = 0
        var level = PasswordStrengthLevel.WEAK
        var text = "Weak"

        if (password.length >= 6) score += 20
        if (password.length >= 8) score += 10
        if (password.length >= 12) score += 10

        if (password.any { it.isLowerCase() }) score += 10
        if (password.any { it.isUpperCase() }) score += 10
        if (password.any { it.isDigit() }) score += 10
        if (password.any { !it.isLetterOrDigit() }) score += 10

        when {
            score >= 70 -> {
                level = PasswordStrengthLevel.VERY_STRONG
                text = "Very Strong"
            }
            score >= 50 -> {
                level = PasswordStrengthLevel.STRONG
                text = "Strong"
            }
            score >= 30 -> {
                level = PasswordStrengthLevel.MEDIUM
                text = "Medium"
            }
            else -> {
                level = PasswordStrengthLevel.WEAK
                text = "Weak"
            }
        }

        return PasswordStrength(level, score, text)
    }

    private data class PasswordStrength(
        val level: PasswordStrengthLevel,
        val percentage: Int,
        val text: String
    )

    private enum class PasswordStrengthLevel {
        WEAK, MEDIUM, STRONG, VERY_STRONG
    }

    private fun showError(errorTextView: android.widget.TextView, message: String) {
        errorTextView.text = message
        errorTextView.visibility = android.view.View.VISIBLE
    }

    private fun hideError(errorTextView: android.widget.TextView) {
        errorTextView.visibility = android.view.View.GONE
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            Pattern.CASE_INSENSITIVE
        )
        return emailPattern.matcher(email).matches()
    }

    private fun register(username: String, email: String, password: String) {
        binding.registerButton.isEnabled = false
        binding.registerButton.text = "Registering..."

        lifecycleScope.launch {
            authRepository.register(username, email, password)
                .onSuccess { authResponse ->
                    tokenManager.saveToken(authResponse.token)
                    tokenManager.saveUserId(authResponse.userId.toString())
                    tokenManager.saveUserInfo(
                        username = authResponse.username,
                        email = authResponse.email,
                        firstName = authResponse.firstName,
                        lastName = authResponse.lastName
                    )

                    Toast.makeText(this@RegisterActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
                .onFailure { error ->
                    Toast.makeText(
                        this@RegisterActivity,
                        "Error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.registerButton.isEnabled = true
                    binding.registerButton.text = "Register"
                }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}