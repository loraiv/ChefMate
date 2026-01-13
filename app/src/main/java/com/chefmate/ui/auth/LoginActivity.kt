package com.chefmate.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chefmate.databinding.ActivityLoginBinding
import com.chefmate.data.repository.AuthRepository
import com.chefmate.ui.main.MainActivity
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityLoginBinding.inflate(layoutInflater)
            setContentView(binding.root)

            tokenManager = TokenManager(this)
            authRepository = AuthRepository()

            val token = tokenManager.getToken()
            
            if (!token.isNullOrEmpty()) {
                navigateToMain()
                return
            }

            setupClickListeners()
            setupRealTimeValidation()
            loadRememberedCredentials()
        } catch (e: Exception) {
            Toast.makeText(this, "Грешка при зареждане: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Моля попълнете всички полета", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            login(email, password)
        }

        binding.forgotPasswordTextView.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        binding.registerTextView.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRealTimeValidation() {
        binding.emailEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val email = s?.toString()?.trim() ?: ""
                if (email.isNotEmpty() && !isValidEmail(email)) {
                    showError(binding.emailErrorText, "Моля въведете валиден имейл адрес")
                } else {
                    hideError(binding.emailErrorText)
                }
            }
        })
    }

    private fun loadRememberedCredentials() {
        if (tokenManager.isRememberMeEnabled()) {
            val rememberedEmail = tokenManager.getRememberedEmail()
            if (!rememberedEmail.isNullOrEmpty()) {
                binding.emailEditText.setText(rememberedEmail)
                binding.rememberMeCheckBox.isChecked = true
            }
        }
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

    private fun login(email: String, password: String) {
        binding.loginButton.isEnabled = false
        binding.loginButton.text = "Влизане..."

        lifecycleScope.launch {
            authRepository.login(email, password)
                .onSuccess { authResponse ->
                    tokenManager.saveToken(authResponse.token)
                    tokenManager.saveUserId(authResponse.userId.toString())
                    tokenManager.saveUserInfo(
                        username = authResponse.username,
                        email = authResponse.email,
                        firstName = authResponse.firstName,
                        lastName = authResponse.lastName
                    )

                    if (binding.rememberMeCheckBox.isChecked) {
                        tokenManager.saveRememberMeCredentials(email, password)
                    } else {
                        tokenManager.clearRememberMeCredentials()
                    }

                    Toast.makeText(this@LoginActivity, "Успешен вход!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
                .onFailure { error ->
                    Toast.makeText(
                        this@LoginActivity,
                        "Грешка: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.loginButton.isEnabled = true
                    binding.loginButton.text = "Влез"
                }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}