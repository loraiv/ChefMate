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

            setupClickListeners()
            setupRealTimeValidation()
            setupPasswordToggle()
            
            binding.root.post {
                loadRememberedCredentials()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to load the login screen. Please try again.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
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
                    showError(binding.emailErrorText, "Please enter a valid email address")
                } else {
                    hideError(binding.emailErrorText)
                }
            }
        })
    }

    private fun setupPasswordToggle() {
        binding.passwordInputLayout.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM
        binding.passwordInputLayout.setEndIconDrawable(com.google.android.material.R.drawable.design_ic_visibility_off)
        
        var isPasswordVisible = false
        
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
    }

    private fun loadRememberedCredentials() {
        try {
            val rememberedEmail = tokenManager.getRememberedEmail()
            val rememberedPassword = tokenManager.getRememberedPassword()
            val isRememberMeEnabled = tokenManager.isRememberMeEnabled()
            
            val prefs = getSharedPreferences("ChefMatePrefs", android.content.Context.MODE_PRIVATE)
            val directEmail = prefs.getString("remembered_email", null)
            val directPassword = prefs.getString("remembered_password", null)
            val directFlag = prefs.getBoolean("remember_me", false)
            
            Log.d("LoginActivity", "=== Loading credentials ===")
            Log.d("LoginActivity", "Via TokenManager - Remember me flag: $isRememberMeEnabled")
            Log.d("LoginActivity", "Direct from SharedPreferences - Flag: $directFlag, Email: ${if (directEmail.isNullOrEmpty()) "empty" else directEmail}")
            Log.d("LoginActivity", "All remember_me keys: ${prefs.all.keys.filter { it.contains("remember") }}")
            Log.d("LoginActivity", "Email: ${if (rememberedEmail.isNullOrEmpty()) "empty" else rememberedEmail}")
            Log.d("LoginActivity", "Password: ${if (rememberedPassword.isNullOrEmpty()) "empty" else "filled (${rememberedPassword.length} chars)"}")
            
            if (!rememberedEmail.isNullOrEmpty()) {
                binding.emailEditText.setText(rememberedEmail)
                binding.rememberMeCheckBox.isChecked = isRememberMeEnabled
                
                if (!rememberedPassword.isNullOrEmpty() && rememberedPassword.trim().isNotEmpty()) {
                    binding.passwordEditText.setText(rememberedPassword)
                    Log.d("LoginActivity", "Password filled successfully")
                } else {
                    Log.d("LoginActivity", "Password is empty or null")
                }
                Log.d("LoginActivity", "Credentials loaded successfully")
            } else {
                Log.d("LoginActivity", "No saved credentials found")
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error loading remembered credentials", e)
            e.printStackTrace()
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
        val existingToken = tokenManager.getToken()
        if (!existingToken.isNullOrEmpty()) {
            Log.d("LoginActivity", "Clearing existing token before new login")
            tokenManager.clearToken()
            tokenManager.clearUserInfo()
        }
        
        binding.loginButton.isEnabled = false
        binding.loginButton.text = "Logging in..."

        lifecycleScope.launch {
            Log.d("LoginActivity", "=== STARTING LOGIN ===")
            Log.d("LoginActivity", "Email: $email")
            Log.d("LoginActivity", "Remember me checkbox state BEFORE login: ${binding.rememberMeCheckBox.isChecked}")
            authRepository.login(email, password)
                .onSuccess { authResponse ->
                    Log.d("LoginActivity", "Login successful! Saving token and user info...")
                    tokenManager.saveToken(authResponse.token)
                    tokenManager.saveUserId(authResponse.userId.toString())
                    tokenManager.saveUserInfo(
                        username = authResponse.username,
                        email = authResponse.email,
                        firstName = authResponse.firstName,
                        lastName = authResponse.lastName,
                        role = authResponse.role
                    )

                    // Save remember me credentials BEFORE navigating
                    val isChecked = binding.rememberMeCheckBox.isChecked
                    Log.d("LoginActivity", "=== Remember Me Check ===")
                    Log.d("LoginActivity", "Checkbox state: $isChecked")
                    Log.d("LoginActivity", "Email: $email")
                    
                    if (isChecked) {
                        Log.d("LoginActivity", "Checkbox is checked - Saving remember me credentials for: $email")
                        
                        tokenManager.saveRememberMeCredentials(email, password)
                        
                        // Verify immediately
                        val savedEmail = tokenManager.getRememberedEmail()
                        val savedPassword = tokenManager.getRememberedPassword()
                        val isEnabled = tokenManager.isRememberMeEnabled()
                        
                        Log.d("LoginActivity", "Immediate verification after save:")
                        Log.d("LoginActivity", "  - Email saved: ${if (savedEmail.isNullOrEmpty()) "NO" else savedEmail}")
                        Log.d("LoginActivity", "  - Password saved: ${if (savedPassword.isNullOrEmpty()) "NO" else "YES (${savedPassword.length} chars)"}")
                        Log.d("LoginActivity", "  - Remember me flag: $isEnabled")
                        
                        if (savedEmail.isNullOrEmpty() || !isEnabled) {
                            Log.e("LoginActivity", "ERROR: Credentials were NOT saved properly!")
                        } else {
                            Log.d("LoginActivity", "SUCCESS: Credentials saved properly!")
                        }
                    } else {
                        Log.d("LoginActivity", "Checkbox is NOT checked - Clearing remember me credentials")
                        tokenManager.clearRememberMeCredentials()
                    }

                    Toast.makeText(this@LoginActivity, "Welcome back! Login successful.", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
                .onFailure { error ->
                    val userFriendlyMessage = when {
                        error.message?.contains("Invalid", ignoreCase = true) == true -> 
                            "Invalid email or password. Please check your credentials and try again."
                        error.message?.contains("Cannot connect", ignoreCase = true) == true -> 
                            error.message ?: "Cannot connect to server. Please check if the backend is running."
                        error.message?.contains("Connection refused", ignoreCase = true) == true -> 
                            "Connection refused. Please check if the backend server is running on port 8090."
                        error.message?.contains("timeout", ignoreCase = true) == true -> 
                            "Request timed out. Please check your internet connection and try again."
                        error.message?.contains("network", ignoreCase = true) == true -> 
                            error.message ?: "Network error. Please check your internet connection and try again."
                        else -> 
                            error.message ?: "Unable to sign in. Please try again later."
                    }
                    Toast.makeText(
                        this@LoginActivity,
                        userFriendlyMessage,
                        Toast.LENGTH_LONG
                    ).show()
                    binding.loginButton.isEnabled = true
                    binding.loginButton.text = "Login"
                }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}