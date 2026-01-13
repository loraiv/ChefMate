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
            Toast.makeText(this, "Грешка при зареждане: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupClickListeners() {
        binding.sendResetButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Моля въведете имейл адрес", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                Toast.makeText(this, "Моля въведете валиден имейл адрес", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendPasswordReset(email)
        }

        binding.backToLoginTextView.setOnClickListener {
            finish()
        }
    }

    private fun sendPasswordReset(email: String) {
        binding.sendResetButton.isEnabled = false
        binding.sendResetButton.text = "Изпращане..."

        lifecycleScope.launch {
            authRepository.forgotPassword(email)
                .onSuccess {
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Инструкциите за възстановяване на паролата са изпратени на вашия имейл адрес.",
                        Toast.LENGTH_LONG
                    ).show()
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        finish()
                    }, 2000)
                }
                .onFailure { error ->
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Грешка: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.sendResetButton.isEnabled = true
                    binding.sendResetButton.text = "Изпрати инструкции"
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

