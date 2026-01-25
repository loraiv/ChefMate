package com.chefmate.ui.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.chefmate.R
import com.chefmate.data.repository.AuthRepository
import com.chefmate.databinding.ActivityProfileBinding
import com.chefmate.ui.auth.LoginActivity
import com.chefmate.ui.main.MainActivity
import com.chefmate.utils.TokenManager
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var tokenManager: TokenManager
    private val authRepository = AuthRepository()
    private var currentImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            imageUri?.let { uri ->
                startCrop(uri)
            }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCameraInternal()
        } else {
            Toast.makeText(
                this,
                "Camera permission is required to take photos",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentImageUri?.let { uri ->
                startCrop(uri)
            }
        }
    }

    private val cropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let { uri ->
                saveProfileImage(uri)
            } ?: run {
                Toast.makeText(this, "Unable to process the image. Please try again.", Toast.LENGTH_SHORT).show()
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR && result.data != null) {
            val cropError = UCrop.getError(result.data!!)
            Toast.makeText(this, "Unable to crop the image. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        setupToolbar()
        loadUserInfo()
        setupProfileImage()
        setupMyRecipes()
        setupAdminPanel()
        setupChangeUsername()
        setupChangePassword()
        setupThemeToggle()
        setupLogout()
        setupDeleteAccount()
    }

    override fun onResume() {
        super.onResume()
        loadProfileImage()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Profile"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadUserInfo() {
        val username = tokenManager.getUsername() ?: "User"
        val email = tokenManager.getEmail() ?: "email@example.com"
        val firstName = tokenManager.getFirstName()
        val lastName = tokenManager.getLastName()

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

    private fun setupProfileImage() {
        binding.profileImageView.setOnClickListener {
            showImageSourceDialog()
        }
    }

    private fun showImageSourceDialog() {
        val options = mutableListOf<String>()
        options.add("Gallery")
        options.add("Camera")
        
        val profileImagePath = tokenManager.getProfileImagePath()
        if (!profileImagePath.isNullOrEmpty()) {
            options.add("Delete Image")
        }

        AlertDialog.Builder(this)
            .setTitle("Select Action")
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> openCamera()
                    2 -> if (options.size > 2) deleteProfileImage()
                }
            }
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun openCamera() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCameraInternal()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCameraInternal() {
        try {
            // Create unique file each time
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_profile_${timeStamp}_"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val photoFile = File.createTempFile(imageFileName, ".jpg", storageDir)
            
            val photoURI = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            currentImageUri = photoURI
            cameraLauncher.launch(photoURI)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open camera. Please check camera permissions.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCrop(sourceUri: Uri) {
        try {
            // Use external files directory for both source and destination
            val externalDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            externalDir?.mkdirs()
            
            // Copy source image to external directory
            val tempSourceFile = File(externalDir, "temp_source_${System.currentTimeMillis()}.jpg")
            
            contentResolver.openInputStream(sourceUri)?.use { input ->
                tempSourceFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                Toast.makeText(this, "Unable to read the selected image. Please try again.", Toast.LENGTH_SHORT).show()
                return
            }
            
            val tempSourceUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                tempSourceFile
            )
            
            // Destination file in external directory
            val destinationFile = File(externalDir, "cropped_profile_${System.currentTimeMillis()}.jpg")
            
            val destinationUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                destinationFile
            )
            
            val uCrop = UCrop.of(tempSourceUri, destinationUri)
                .withAspectRatio(1f, 1f) // Square for profile picture
                .withMaxResultSize(1000, 1000)
            
            val intent = uCrop.getIntent(this)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            cropLauncher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ProfileActivity", "Error in startCrop: ${e.message}", e)
            Toast.makeText(this, "Unable to process the image. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfileImage(uri: Uri) {
        lifecycleScope.launch {
            try {
                val profileImageFile = File(getFilesDir(), "profile_image.jpg")
                
                if (profileImageFile.exists()) {
                    profileImageFile.delete()
                }
                
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(profileImageFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: run {
                    Toast.makeText(this@ProfileActivity, "Unable to read the selected image. Please try again.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val token = tokenManager.getToken()
                if (token != null) {
                    val result = authRepository.uploadProfileImage(token, profileImageFile)
                    result.onSuccess { imageUrl ->
                        Log.d("ProfileActivity", "Profile image uploaded successfully: $imageUrl")
                    }.onFailure { error ->
                        Log.e("ProfileActivity", "Failed to upload profile image: ${error.message}", error)
                    }
                }
                
                tokenManager.saveProfileImagePath(profileImageFile.absolutePath)
                
                Glide.with(this@ProfileActivity).clear(binding.profileImageView)
                
                Glide.with(this@ProfileActivity)
                    .load(profileImageFile)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                    .circleCrop()
                    .into(binding.profileImageView)
                
                Toast.makeText(this@ProfileActivity, "Profile picture updated successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ProfileActivity", "Error saving profile image: ${e.message}", e)
                Toast.makeText(this@ProfileActivity, "Unable to save profile picture. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProfileImage() {
        Glide.with(this).clear(binding.profileImageView)
        
        val profileImagePath = tokenManager.getProfileImagePath()
        if (!profileImagePath.isNullOrEmpty()) {
            val file = File(profileImagePath)
            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                    .circleCrop()
                    .into(binding.profileImageView)
            } else {
                tokenManager.saveProfileImagePath(null)
                loadDefaultImage()
            }
        } else {
            loadDefaultImage()
        }
    }

    private fun loadDefaultImage() {
        binding.profileImageView.setImageResource(R.drawable.ic_user_placeholder_gray)
    }

    private fun deleteProfileImage() {
        AlertDialog.Builder(this)
            .setTitle("Delete Image")
            .setMessage("Are you sure you want to delete your profile picture?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val profileImagePath = tokenManager.getProfileImagePath()
                        if (!profileImagePath.isNullOrEmpty()) {
                            val file = File(profileImagePath)
                            if (file.exists()) {
                                file.delete()
                            }
                        }
                        tokenManager.saveProfileImagePath(null)
                        
                        val token = tokenManager.getToken()
                        if (token != null) {
                            val result = authRepository.deleteProfileImage(token)
                            result.onSuccess {
                                Log.d("ProfileActivity", "Profile image deleted from backend successfully")
                            }.onFailure { error ->
                                Log.e("ProfileActivity", "Failed to delete profile image from backend: ${error.message}", error)
                            }
                        }
                        
                        Glide.with(this@ProfileActivity).clear(binding.profileImageView)
                        
                        loadDefaultImage()
                        Toast.makeText(this@ProfileActivity, "Profile picture removed successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("ProfileActivity", "Error deleting profile image: ${e.message}", e)
                        Toast.makeText(this@ProfileActivity, "Unable to delete profile picture. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun setupMyRecipes() {
        if (tokenManager.isAdmin()) {
            binding.myRecipesButton.visibility = View.GONE
            return
        }
        
        binding.myRecipesButton.setOnClickListener {
            val userId = tokenManager.getUserId()?.toLongOrNull()
            if (userId != null) {
                val username = tokenManager.getUsername() ?: "My Recipes"
                val intent = Intent(this, com.chefmate.ui.main.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("navigateTo", "userRecipes")
                    putExtra("userId", userId)
                    putExtra("username", username)
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Please sign in to continue", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupAdminPanel() {
        val userRole = tokenManager.getUserRole()
        val isAdmin = tokenManager.isAdmin()
        Log.d("ProfileActivity", "User role: $userRole, isAdmin: $isAdmin")
        
        if (isAdmin) {
            binding.adminPanelButton.visibility = View.VISIBLE
            binding.adminPanelButton.setOnClickListener {
                navigateToAdminPanel()
            }
        } else {
            binding.adminPanelButton.visibility = View.GONE
        }
    }

    private fun navigateToAdminPanel() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("navigateTo", "adminPanel")
        }
        startActivity(intent)
        finish()
    }

    private fun setupChangeUsername() {
        if (tokenManager.isAdmin()) {
            binding.changeUsernameButton.visibility = android.view.View.GONE
            return
        }

        binding.changeUsernameButton.setOnClickListener {
            showChangeUsernameDialog()
        }
    }

    private fun showChangeUsernameDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_username_change, null)
        val editNewUsername = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editNewUsername)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Change Username")
            .setView(dialogView)
            .setPositiveButton("Change") { _, _ ->
                val newUsername = editNewUsername.text?.toString()?.trim() ?: ""
                if (validateUsernameChange(newUsername)) {
                    changeUsername(newUsername)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun validateUsernameChange(newUsername: String): Boolean {
        if (newUsername.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }
        if (newUsername.length < 3) {
            Toast.makeText(this, "Username must be at least 3 characters", Toast.LENGTH_SHORT).show()
            return false
        }
        if (newUsername == tokenManager.getUsername()) {
                Toast.makeText(this, "New username must be different from your current username", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun changeUsername(newUsername: String) {
        val token = tokenManager.getToken() ?: return
        binding.changeUsernameButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val result = authRepository.changeUsername(token, newUsername)
                result.fold(
                    onSuccess = { username ->
                        tokenManager.saveUsername(username)
                        loadUserInfo()
                        Toast.makeText(this@ProfileActivity, "Username updated successfully", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { exception ->
                        val userFriendlyMessage = when {
                            exception.message?.contains("already taken", ignoreCase = true) == true -> 
                                "This username is already taken. Please choose a different one."
                            exception.message?.contains("network", ignoreCase = true) == true -> 
                                "Network error. Please check your internet connection and try again."
                            else -> 
                                "Unable to change username. Please try again later."
                        }
                        Toast.makeText(this@ProfileActivity, userFriendlyMessage, Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "An unexpected error occurred. Please try again.", Toast.LENGTH_SHORT).show()
            } finally {
                binding.changeUsernameButton.isEnabled = true
            }
        }
    }

    private fun setupChangePassword() {
        binding.changePasswordButton.setOnClickListener {
            showChangePasswordDialog()
        }
    }

    private fun setupThemeToggle() {
        updateThemeButtonText()
        binding.themeToggleButton.setOnClickListener {
            showThemeSelectionDialog()
        }
    }

    private fun updateThemeButtonText() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val buttonText = when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> "Dark Theme"
            AppCompatDelegate.MODE_NIGHT_NO -> "Light Theme"
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> {
                // Check system theme to show current state
                val nightModeFlags = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                    "Dark Theme (System)"
                } else {
                    "Light Theme (System)"
                }
            }
            else -> "Theme"
        }
        
        binding.themeToggleButton.text = buttonText
    }

    private fun showThemeSelectionDialog() {
        val themes = arrayOf("Light Theme", "Dark Theme", "Follow System Setting")
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val selectedIndex = when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> 0
            AppCompatDelegate.MODE_NIGHT_YES -> 1
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> 2
            else -> 2
        }

        AlertDialog.Builder(this)
            .setTitle("Select Theme")
            .setSingleChoiceItems(themes, selectedIndex) { dialog, which ->
                val newMode = when (which) {
                    0 -> AppCompatDelegate.MODE_NIGHT_NO
                    1 -> AppCompatDelegate.MODE_NIGHT_YES
                    2 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                
                tokenManager.saveThemeMode(newMode)
                
                AppCompatDelegate.setDefaultNightMode(newMode)
                
                updateThemeButtonText()
                
                dialog.dismiss()
                
                recreate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(com.chefmate.R.layout.dialog_change_password, null)
        val currentPasswordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.chefmate.R.id.currentPasswordEditText)
        val newPasswordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.chefmate.R.id.newPasswordEditText)
        val confirmPasswordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.chefmate.R.id.confirmPasswordEditText)
        
        val currentPasswordLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(com.chefmate.R.id.currentPasswordInputLayout)
        val newPasswordLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(com.chefmate.R.id.newPasswordInputLayout)
        val confirmPasswordLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(com.chefmate.R.id.confirmPasswordInputLayout)

        setupPasswordToggleForDialog(currentPasswordLayout, currentPasswordInput)
        setupPasswordToggleForDialog(newPasswordLayout, newPasswordInput)
        setupPasswordToggleForDialog(confirmPasswordLayout, confirmPasswordInput)

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change") { _, _ ->
                val currentPassword = currentPasswordInput?.text?.toString() ?: ""
                val newPassword = newPasswordInput?.text?.toString() ?: ""
                val confirmPassword = confirmPasswordInput?.text?.toString() ?: ""

                if (validatePasswordChange(currentPassword, newPassword, confirmPassword)) {
                    changePassword(currentPassword, newPassword)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun setupPasswordToggleForDialog(
        layout: com.google.android.material.textfield.TextInputLayout?,
        editText: com.google.android.material.textfield.TextInputEditText?
    ) {
        if (layout == null || editText == null) return
        
        layout.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM
        layout.setEndIconDrawable(com.google.android.material.R.drawable.design_ic_visibility_off)
        
        var isPasswordVisible = false
        
        layout.setEndIconOnClickListener {
            isPasswordVisible = !isPasswordVisible
            
            if (isPasswordVisible) {
                editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                layout.setEndIconDrawable(com.google.android.material.R.drawable.design_ic_visibility)
            } else {
                editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                layout.setEndIconDrawable(com.google.android.material.R.drawable.design_ic_visibility_off)
            }
            editText.setSelection(editText.text?.length ?: 0)
        }
    }

    private fun validatePasswordChange(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Boolean {
        if (currentPassword.isEmpty()) {
            Toast.makeText(this, "Please enter current password", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword.isEmpty() || newPassword.length < 6) {
            Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }

        if (currentPassword == newPassword) {
            Toast.makeText(this, "New password must be different from current password", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        binding.changePasswordButton.isEnabled = false

        lifecycleScope.launch {
            val token = tokenManager.getToken()
            if (token == null) {
                Toast.makeText(this@ProfileActivity, "Error: You are not logged in", Toast.LENGTH_SHORT).show()
                binding.changePasswordButton.isEnabled = true
                return@launch
            }

            val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            authRepository.changePassword(authToken, currentPassword, newPassword)
                .onSuccess {
                    Toast.makeText(this@ProfileActivity, "Password changed successfully", Toast.LENGTH_SHORT).show()
                }
                .onFailure { error ->
                    val userFriendlyMessage = when {
                        error.message?.contains("incorrect", ignoreCase = true) == true -> 
                            "Current password is incorrect. Please try again."
                        error.message?.contains("must be different", ignoreCase = true) == true -> 
                            "New password must be different from your current password."
                        error.message?.contains("network", ignoreCase = true) == true -> 
                            "Network error. Please check your internet connection and try again."
                        else -> 
                            "Unable to change password. Please try again later."
                    }
                    Toast.makeText(this@ProfileActivity, userFriendlyMessage, Toast.LENGTH_LONG).show()
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
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                logout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun logout() {
        tokenManager.clearToken()
        tokenManager.clearUserInfo()
        tokenManager.clearRememberMeCredentials()

        Toast.makeText(this, "You have been signed out successfully", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupDeleteAccount() {
        binding.deleteAccountButton.setOnClickListener {
            showDeleteAccountConfirmation()
        }
    }

    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account?\n\n" +
                    "This action is irreversible and will delete:\n" +
                    "• Your profile\n" +
                    "• All your recipes\n" +
                    "• All your comments\n" +
                    "• All your shopping lists")
            .setPositiveButton("Delete", null)
            .setNegativeButton("Cancel", null)
            .show()
            .apply {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    dismiss()
                    showFinalDeleteConfirmation()
                }
            }
    }

    private fun showFinalDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Final Warning")
            .setMessage("This action is final and irreversible!\n\n" +
                    "All your data will be permanently deleted.")
            .setPositiveButton("Yes, delete account", null)
            .setNegativeButton("Cancel", null)
            .show()
            .apply {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    dismiss()
                    deleteAccount()
                }
            }
    }

    private fun deleteAccount() {
        binding.deleteAccountButton.isEnabled = false
        binding.deleteAccountButton.text = "Deleting..."

        lifecycleScope.launch {
            val token = tokenManager.getToken()
            if (token == null) {
                Toast.makeText(this@ProfileActivity, "Error: You are not logged in", Toast.LENGTH_SHORT).show()
                binding.deleteAccountButton.isEnabled = true
                binding.deleteAccountButton.text = "Delete Account"
                return@launch
            }

            val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            authRepository.deleteAccount(authToken)
                .onSuccess {
                    // Clear all local data
                    tokenManager.clearToken()
                    tokenManager.clearUserInfo()
                    tokenManager.clearRememberMeCredentials()
                    tokenManager.saveProfileImagePath(null)

                    Toast.makeText(
                        this@ProfileActivity,
                        "Account deleted successfully",
                        Toast.LENGTH_LONG
                    ).show()

                    val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .onFailure { error ->
                    val userFriendlyMessage = when {
                        error.message?.contains("network", ignoreCase = true) == true -> 
                            "Network error. Please check your internet connection and try again."
                        else -> 
                            "Unable to delete account. Please try again later."
                    }
                    Toast.makeText(this@ProfileActivity, userFriendlyMessage, Toast.LENGTH_LONG).show()
                    binding.deleteAccountButton.isEnabled = true
                    binding.deleteAccountButton.text = "Delete Account"
                }
        }
    }
}

