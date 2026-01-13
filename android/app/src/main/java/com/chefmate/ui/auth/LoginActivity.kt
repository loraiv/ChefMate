package com.chefmate.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chefmate.databinding.ActivityLoginBinding
import com.chefmate.data.repository.AuthRepository
import com.chefmate.ui.main.MainActivity
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        authRepository = AuthRepository()

        // Провери дали вече има токен
        if (!tokenManager.getToken().isNullOrEmpty()) {
            navigateToMain()
            return
        }

        setupClickListeners()
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

        binding.registerTextView.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun login(email: String, password: String) {
        binding.loginButton.isEnabled = false
        binding.loginButton.text = "Влизане..."

        lifecycleScope.launch {
            authRepository.login(email, password)
                .onSuccess { authResponse ->
                    // Запази токена
                    tokenManager.saveToken(authResponse.token)
                    tokenManager.saveUserId(authResponse.userId.toString())

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