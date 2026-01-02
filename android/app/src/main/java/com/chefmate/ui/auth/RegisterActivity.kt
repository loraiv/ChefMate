package com.chefmate.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chefmate.databinding.ActivityRegisterBinding
import com.chefmate.data.repository.AuthRepository
import com.chefmate.ui.main.MainActivity
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.launch

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
    }

    private fun setupClickListeners() {
        binding.registerButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Моля попълнете всички полета", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Паролата трябва да е поне 6 символа", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            register(username, email, password)
        }
    }

    private fun register(username: String, email: String, password: String) {
        binding.registerButton.isEnabled = false
        binding.registerButton.text = "Регистриране..."

        lifecycleScope.launch {
            authRepository.register(username, email, password)
                .onSuccess { authResponse ->
                    // Запази токена
                    tokenManager.saveToken(authResponse.token)
                    tokenManager.saveUserId(authResponse.userId.toString())

                    Toast.makeText(this@RegisterActivity, "Успешна регистрация!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
                .onFailure { error ->
                    Toast.makeText(
                        this@RegisterActivity,
                        "Грешка: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.registerButton.isEnabled = true
                    binding.registerButton.text = "Регистрирай се"
                }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}