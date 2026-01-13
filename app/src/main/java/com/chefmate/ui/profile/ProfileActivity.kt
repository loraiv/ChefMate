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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.chefmate.data.repository.AuthRepository
import com.chefmate.databinding.ActivityProfileBinding
import com.chefmate.ui.auth.LoginActivity
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
                "Разрешението за камерата е необходимо за снимане на снимки",
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
                Toast.makeText(this, "Грешка при обработка на снимката", Toast.LENGTH_SHORT).show()
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR && result.data != null) {
            val cropError = UCrop.getError(result.data!!)
            Toast.makeText(this, "Грешка при обработка на снимката: ${cropError?.message}", Toast.LENGTH_SHORT).show()
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
        setupChangePassword()
        setupLogout()
    }

    override fun onResume() {
        super.onResume()
        // Reload profile image every time the activity resumes to show latest image
        loadProfileImage()
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
        options.add("Галерия")
        options.add("Камера")
        
        // Add delete option if image exists
        val profileImagePath = tokenManager.getProfileImagePath()
        if (!profileImagePath.isNullOrEmpty()) {
            options.add("Изтрий снимка")
        }

        AlertDialog.Builder(this)
            .setTitle("Избери действие")
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
            Toast.makeText(this, "Грешка при отваряне на камерата: ${e.message}", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Грешка при четене на снимката", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Грешка при обработка на снимката: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfileImage(uri: Uri) {
        lifecycleScope.launch {
            try {
                val profileImageFile = File(getFilesDir(), "profile_image.jpg")
                
                // Delete old file if exists
                if (profileImageFile.exists()) {
                    profileImageFile.delete()
                }
                
                // Copy image to app's internal storage
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(profileImageFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: run {
                    Toast.makeText(this@ProfileActivity, "Грешка при четене на снимката", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Upload to backend
                val token = tokenManager.getToken()
                if (token != null) {
                    val result = authRepository.uploadProfileImage(token, profileImageFile)
                    result.onSuccess { imageUrl ->
                        Log.d("ProfileActivity", "Profile image uploaded successfully: $imageUrl")
                    }.onFailure { error ->
                        Log.e("ProfileActivity", "Failed to upload profile image: ${error.message}", error)
                        // Still save locally even if upload fails
                    }
                }
                
                // Save path to SharedPreferences
                tokenManager.saveProfileImagePath(profileImageFile.absolutePath)
                
                // Clear Glide cache for this image
                Glide.with(this@ProfileActivity).clear(binding.profileImageView)
                
                // Load and display image with skipMemoryCache to force reload
                Glide.with(this@ProfileActivity)
                    .load(profileImageFile)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                    .circleCrop()
                    .into(binding.profileImageView)
                
                Toast.makeText(this@ProfileActivity, "Профилната снимка е запазена", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ProfileActivity", "Error saving profile image: ${e.message}", e)
                Toast.makeText(this@ProfileActivity, "Грешка при запазване на снимката: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProfileImage() {
        // Clear Glide cache first to ensure fresh load
        Glide.with(this).clear(binding.profileImageView)
        
        val profileImagePath = tokenManager.getProfileImagePath()
        if (!profileImagePath.isNullOrEmpty()) {
            val file = File(profileImagePath)
            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .skipMemoryCache(true) // Skip memory cache to always show latest
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE) // Skip disk cache
                    .circleCrop()
                    .into(binding.profileImageView)
            } else {
                // If file doesn't exist, clear the saved path
                tokenManager.saveProfileImagePath(null)
                loadDefaultImage()
            }
        } else {
            loadDefaultImage()
        }
    }

    private fun loadDefaultImage() {
        binding.profileImageView.setImageResource(android.R.drawable.ic_menu_myplaces)
    }

    private fun deleteProfileImage() {
        AlertDialog.Builder(this)
            .setTitle("Изтриване на снимка")
            .setMessage("Сигурни ли сте, че искате да изтриете профилната си снимка?")
            .setPositiveButton("Да") { _, _ ->
                val profileImagePath = tokenManager.getProfileImagePath()
                if (!profileImagePath.isNullOrEmpty()) {
                    val file = File(profileImagePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                tokenManager.saveProfileImagePath(null)
                loadDefaultImage()
                Toast.makeText(this, "Профилната снимка е изтрита", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Не", null)
            .show()
    }

    private fun setupMyRecipes() {
        binding.myRecipesButton.setOnClickListener {
            val userId = tokenManager.getUserId()?.toLongOrNull()
            if (userId != null) {
                val username = tokenManager.getUsername() ?: "Моите рецепти"
                // Navigate to MainActivity and then to user recipes
                val intent = Intent(this, com.chefmate.ui.main.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("navigateTo", "userRecipes")
                    putExtra("userId", userId)
                    putExtra("username", username)
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Грешка: Не сте влезли в системата", Toast.LENGTH_SHORT).show()
            }
        }
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

