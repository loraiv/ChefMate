package com.chefmate.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chefmate.data.repository.AuthRepository
import com.chefmate.databinding.ActivityProfileBinding
import com.chefmate.ui.auth.LoginActivity
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var tokenManager: TokenManager
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        setupToolbar()
        loadUserInfo()
        setupChangePassword()
        setupLogout()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Моят профил"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadUserInfo() {
        val username = tokenManager.getUsername() ?: "Потребител"
        val email = tokenManager.getEmail() ?: "email@example.com"
        val firstName = tokenManager.getFirstName()
        val lastName = tokenManager.getLastName()

        // Покажи име (firstName + lastName или username)
        val displayName = if (!firstName.isNullOrEmpty() && !lastName.isNullOrEmpty()) {
            "$firstName $lastName"
        } else if (!firstName.isNullOrEmpty()) {
            firstName
        } else {
            username
        }

        binding.profileNameTextView.text = displayName
        binding.profileEmailTextView.text = email
    }

    private fun setupChangePassword() {
        binding.changePasswordButton.setOnClickListener {
            showChangePasswordDialog()
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(com.chefmate.R.layout.dialog_change_password, null)
        val currentPasswordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.chefmate.R.id.currentPasswordEditText)
        val newPasswordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.chefmate.R.id.newPasswordEditText)
        val confirmPasswordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.chefmate.R.id.confirmPasswordEditText)

        AlertDialog.Builder(this)
            .setTitle("Смяна на парола")
            .setView(dialogView)
            .setPositiveButton("Промени") { _, _ ->
                val currentPassword = currentPasswordInput?.text?.toString() ?: ""
                val newPassword = newPasswordInput?.text?.toString() ?: ""
                val confirmPassword = confirmPasswordInput?.text?.toString() ?: ""

                if (validatePasswordChange(currentPassword, newPassword, confirmPassword)) {
                    changePassword(currentPassword, newPassword)
                }
            }
            .setNegativeButton("Отказ", null)
            .show()
    }

    private fun validatePasswordChange(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Boolean {
        if (currentPassword.isEmpty()) {
            Toast.makeText(this, "Моля, въведете текущата парола", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword.isEmpty() || newPassword.length < 6) {
            Toast.makeText(this, "Новата парола трябва да е поне 6 символа", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "Паролите не съвпадат", Toast.LENGTH_SHORT).show()
            return false
        }

        if (currentPassword == newPassword) {
            Toast.makeText(this, "Новата парола трябва да е различна от текущата", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        binding.changePasswordButton.isEnabled = false

        lifecycleScope.launch {
            val token = tokenManager.getToken()
            if (token == null) {
                Toast.makeText(this@ProfileActivity, "Грешка: Не сте влезли в системата", Toast.LENGTH_SHORT).show()
                binding.changePasswordButton.isEnabled = true
                return@launch
            }

            val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            authRepository.changePassword(authToken, currentPassword, newPassword)
                .onSuccess {
                    Toast.makeText(this@ProfileActivity, "Паролата е успешно променена!", Toast.LENGTH_SHORT).show()
                }
                .onFailure { error ->
                    val errorMessage = error.message ?: "Грешка при смяна на парола"
                    Toast.makeText(this@ProfileActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            
            binding.changePasswordButton.isEnabled = true
        }
    }

    private fun setupLogout() {
        binding.logoutButton.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Изход")
            .setMessage("Сигурни ли сте, че искате да излезете?")
            .setPositiveButton("Да") { _, _ ->
                logout()
            }
            .setNegativeButton("Не", null)
            .show()
    }

    private fun logout() {
        tokenManager.clearToken()
        tokenManager.clearUserInfo()
        tokenManager.clearRememberMeCredentials()

        Toast.makeText(this, "Излязохте успешно", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

