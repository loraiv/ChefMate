package com.chefmate.ui.recipes

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chefmate.R
import com.chefmate.data.repository.RecipeRepository
import com.chefmate.databinding.FragmentAddRecipeBinding
import com.chefmate.di.AppModule
import com.chefmate.ui.recipes.adapter.IngredientAdapter
import com.chefmate.ui.recipes.adapter.RecipeImageAdapter
import com.chefmate.ui.recipes.adapter.StepAdapter
import com.chefmate.data.api.models.RecipeResponse
import com.chefmate.ui.recipes.viewmodel.AddRecipeViewModel
import com.chefmate.ui.recipes.viewmodel.AddRecipeViewModelFactory
import com.chefmate.utils.TokenManager
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddRecipeFragment : Fragment() {

    private var _binding: FragmentAddRecipeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddRecipeViewModel by viewModels {
        val tokenManager = TokenManager(requireContext())
        val apiService = AppModule.provideApiService()
        val recipeRepository = RecipeRepository(apiService, tokenManager)
        AddRecipeViewModelFactory(recipeRepository)
    }
    
    private val recipeRepository: RecipeRepository by lazy {
        val tokenManager = TokenManager(requireContext())
        val apiService = AppModule.provideApiService()
        RecipeRepository(apiService, tokenManager)
    }
    
    private val selectedImagePaths = mutableListOf<String>()
    private val selectedImageUris = mutableListOf<Uri>()
    private var currentCameraImageUri: Uri? = null
    private val ingredients = mutableListOf<String>()
    private val steps = mutableListOf<String>()
    private lateinit var ingredientAdapter: IngredientAdapter
    private lateinit var stepAdapter: StepAdapter
    private var isEditMode = false
    private var recipeId: Long = -1
    private val existingImageUrls = mutableListOf<String>()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            imageUri?.let { uri ->
                if (selectedImageUris.size < 5) {
                    startCrop(uri)
                } else {
                    Toast.makeText(requireContext(), "You can add up to 5 images", Toast.LENGTH_SHORT).show()
                }
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
                requireContext(),
                "Camera permission is required to take photos",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentCameraImageUri?.let { uri ->
                if (selectedImageUris.size < 5) {
                    startCrop(uri)
                } else {
                    Toast.makeText(requireContext(), "You can add up to 5 images", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val cropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let { uri ->
                val path = getRealPathFromURI(uri)
                path?.let {
                    selectedImageUris.add(uri)
                    selectedImagePaths.add(it)
                    updateImagePreview()
                } ?: run {
                    Toast.makeText(requireContext(), "Unable to process the image. Please try again.", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(requireContext(), "Unable to process the image. Please try again.", Toast.LENGTH_SHORT).show()
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR && result.data != null) {
            val cropError = UCrop.getError(result.data!!)
            Toast.makeText(requireContext(), "Unable to crop the image. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddRecipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recipeId = arguments?.getLong("recipeId", -1) ?: -1
        isEditMode = recipeId != -1L

        setupToolbar()
        setupRecyclerViews()
        setupDifficultySpinner()
        setupImagePicker()
        setupAddButtons()
        setupSaveButton()
        setupObservers()
        setupRequiredFieldHints()
        
        if (isEditMode) {
            loadRecipeForEdit(recipeId)
        }
    }

    private fun setupToolbar() {
        val toolbar = binding.toolbar
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.let { activity ->
            activity.setSupportActionBar(toolbar)
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            activity.supportActionBar?.setDisplayShowHomeEnabled(true)
            activity.supportActionBar?.title = if (isEditMode) "Edit Recipe" else "Add Recipe"
        }
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerViews() {
        ingredientAdapter = IngredientAdapter(
            ingredients = ingredients,
            onRemoveClick = { position ->
                ingredients.removeAt(position)
                ingredientAdapter.notifyItemRemoved(position)
            }
        )

        stepAdapter = StepAdapter(
            steps = steps,
            onRemoveClick = { position ->
                steps.removeAt(position)
                stepAdapter.notifyItemRemoved(position)
            }
        )

        binding.ingredientsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.ingredientsRecyclerView.adapter = ingredientAdapter

        binding.stepsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.stepsRecyclerView.adapter = stepAdapter
    }

    private fun setupDifficultySpinner() {
        val difficulties = arrayOf("EASY", "MEDIUM", "HARD")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, difficulties)
        
        binding.difficultySpinner.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Select Difficulty")
                .setItems(difficulties) { _, which ->
                    binding.difficultySpinner.setText(difficulties[which])
                }
                .show()
        }
    }

    private fun setupImagePicker() {
        binding.selectImageButton.setOnClickListener {
            val totalImages = selectedImageUris.size + existingImageUrls.size
            if (totalImages >= 5) {
                Toast.makeText(requireContext(), "You can add up to 5 images", Toast.LENGTH_SHORT).show()
            } else {
                showImageSourceDialog()
            }
        }
        
        if (!isEditMode) {
            if (selectedImageUris.isEmpty()) {
                binding.placeholderImageView.visibility = View.VISIBLE
                binding.recipeImagesViewPager.visibility = View.GONE
            } else {
                binding.placeholderImageView.visibility = View.GONE
                binding.recipeImagesViewPager.visibility = View.VISIBLE
                val adapter = RecipeImageAdapter(
                    imageUrls = selectedImageUris.map { it.toString() },
                    isEditMode = false,
                    onDeleteClick = { position ->
                        handleImageDelete(position)
                    }
                )
                binding.recipeImagesViewPager.adapter = adapter
            }
        }
    }
    
    private fun updateImagePreview() {
        val allImageUrls = if (isEditMode) {
            existingImageUrls + selectedImageUris.map { it.toString() }
        } else {
            selectedImageUris.map { it.toString() }
        }
        
        if (allImageUrls.isEmpty()) {
            binding.placeholderImageView.visibility = View.VISIBLE
            binding.recipeImagesViewPager.visibility = View.GONE
        } else {
            binding.placeholderImageView.visibility = View.GONE
            binding.recipeImagesViewPager.visibility = View.VISIBLE
            
            val adapter = RecipeImageAdapter(
                imageUrls = allImageUrls,
                isEditMode = isEditMode,
                onDeleteClick = { position ->
                    handleImageDelete(position)
                }
            )
            binding.recipeImagesViewPager.adapter = adapter
        }
        
        if (allImageUrls.size > 1) {
            binding.imageIndicatorLayout.visibility = View.VISIBLE
            setupImageIndicators(allImageUrls.size)
        } else {
            binding.imageIndicatorLayout.visibility = View.GONE
        }
        
        binding.selectImageButton.text = "Add Image (${allImageUrls.size}/5)"
    }
    
    private fun handleImageDelete(position: Int) {
        val allImageUrls = if (isEditMode) {
            existingImageUrls + selectedImageUris.map { it.toString() }
        } else {
            selectedImageUris.map { it.toString() }
        }
        
        if (position < 0 || position >= allImageUrls.size) return
        
        val imageUrl = allImageUrls[position]
        
        val isExistingImage = isEditMode && 
            !imageUrl.startsWith("content://") && 
            !imageUrl.startsWith("file://") &&
            existingImageUrls.contains(imageUrl)
        
        if (isExistingImage) {
            existingImageUrls.remove(imageUrl)
        } else {
            val uriString = imageUrl
            val indexToRemove = selectedImageUris.indexOfFirst { it.toString() == uriString }
            if (indexToRemove >= 0) {
                selectedImageUris.removeAt(indexToRemove)
                if (indexToRemove < selectedImagePaths.size) {
                    selectedImagePaths.removeAt(indexToRemove)
                }
            }
        }
        
        updateImagePreview()
    }
    
    private fun setupImageIndicators(count: Int) {
        binding.imageIndicatorLayout.removeAllViews()
        for (i in 0 until count) {
            val dot = View(requireContext()).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4,
                    resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4
                ).apply {
                    setMargins(8, 0, 8, 0)
                }
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(if (i == 0) 0xFF6200EE.toInt() else 0xFFCCCCCC.toInt())
                }
            }
            binding.imageIndicatorLayout.addView(dot)
        }
        
        binding.recipeImagesViewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateImageIndicators(position, count)
            }
        })
    }
    
    private fun updateImageIndicators(selectedPosition: Int, count: Int) {
        for (i in 0 until binding.imageIndicatorLayout.childCount) {
            val dot = binding.imageIndicatorLayout.getChildAt(i)
            val drawable = dot.background as? android.graphics.drawable.GradientDrawable
            drawable?.setColor(if (i == selectedPosition) 0xFF6200EE.toInt() else 0xFFCCCCCC.toInt())
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Gallery", "Camera")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> openCamera()
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
                requireContext(),
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
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_recipe_${timeStamp}_"
            val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val photoFile = File.createTempFile(imageFileName, ".jpg", storageDir)
            
            val photoURI = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            currentCameraImageUri = photoURI
            cameraLauncher.launch(photoURI)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to open camera. Please check camera permissions.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCrop(sourceUri: Uri) {
        try {
            val externalDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            externalDir?.mkdirs()
            
            val tempSourceFile = File(externalDir, "temp_source_${System.currentTimeMillis()}.jpg")
            
            requireContext().contentResolver.openInputStream(sourceUri)?.use { input ->
                tempSourceFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                Toast.makeText(requireContext(), "Unable to read the selected image. Please try again.", Toast.LENGTH_SHORT).show()
                return
            }
            
            val tempSourceUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                tempSourceFile
            )
            
            val destinationFile = File(externalDir, "cropped_recipe_${System.currentTimeMillis()}.jpg")
            
            val destinationUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                destinationFile
            )
            
            val uCrop = UCrop.of(tempSourceUri, destinationUri)
                .withMaxResultSize(2000, 2000)
            
            val intent = uCrop.getIntent(requireContext())
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            
            val aspectRatioList = arrayListOf(
                com.yalantis.ucrop.model.AspectRatio("1:1", 1f, 1f),
                com.yalantis.ucrop.model.AspectRatio("Original", 0f, 0f)
            )
            try {
                intent.putParcelableArrayListExtra("com.yalantis.ucrop.AspectRatioOptions", aspectRatioList)
                intent.putExtra("com.yalantis.ucrop.AspectRatioSelectedByDefault", 0)
            } catch (e: Exception) {
                android.util.Log.w("AddRecipeFragment", "Could not set custom aspect ratios: ${e.message}")
            }
            
            cropLauncher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("AddRecipeFragment", "Error in startCrop: ${e.message}", e)
            Toast.makeText(requireContext(), "Unable to process the image. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAddButtons() {
        binding.addIngredientButton.setOnClickListener {
            showAddIngredientDialog()
        }

        binding.addStepButton.setOnClickListener {
            showAddStepDialog()
        }
    }

    private fun showAddIngredientDialog() {
        val input = android.widget.EditText(requireContext())
        input.hint = "Example: 500g flour"

        AlertDialog.Builder(requireContext())
            .setTitle("Add Ingredient")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val ingredient = input.text.toString().trim()
                if (ingredient.isNotEmpty()) {
                    ingredients.add(ingredient)
                    ingredientAdapter.notifyItemInserted(ingredients.size - 1)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddStepDialog() {
        val input = android.widget.EditText(requireContext())
        input.hint = "Describe the step..."
        input.minLines = 3
        input.maxLines = 5

        AlertDialog.Builder(requireContext())
            .setTitle("Add Step")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val step = input.text.toString().trim()
                if (step.isNotEmpty()) {
                    steps.add(step)
                    stepAdapter.notifyItemInserted(steps.size - 1)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupSaveButton() {
        if (isEditMode) {
            binding.saveRecipeButton.text = "Save Changes"
        }
        binding.saveRecipeButton.setOnClickListener {
            if (validateForm()) {
                saveRecipe()
            }
        }
    }
    
    private fun setupRequiredFieldHints() {
        val errorColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.error)
        
        val titleHint = android.text.SpannableStringBuilder("Recipe Title ")
        titleHint.append("*")
        titleHint.setSpan(
            android.text.style.ForegroundColorSpan(errorColor),
            titleHint.length - 1,
            titleHint.length,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.titleInputLayout.hint = titleHint
        
        // Description is now optional, no asterisk needed
        binding.descriptionInputLayout.hint = "Description (optional)"
        
        // Preparation time is now optional
        binding.prepTimeInputLayout.hint = "Preparation Time (minutes) (optional)"
        
        val cookTimeHint = android.text.SpannableStringBuilder("Cooking Time (minutes) ")
        cookTimeHint.append("*")
        cookTimeHint.setSpan(
            android.text.style.ForegroundColorSpan(errorColor),
            cookTimeHint.length - 1,
            cookTimeHint.length,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.cookTimeInputLayout.hint = cookTimeHint
        
        val diffHint = android.text.SpannableStringBuilder("Difficulty ")
        diffHint.append("*")
        diffHint.setSpan(
            android.text.style.ForegroundColorSpan(errorColor),
            diffHint.length - 1,
            diffHint.length,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.difficultyInputLayout.hint = diffHint
        
        val ingredientsText = android.text.SpannableStringBuilder("Ingredients ")
        ingredientsText.append("*")
        ingredientsText.setSpan(
            android.text.style.ForegroundColorSpan(errorColor),
            ingredientsText.length - 1,
            ingredientsText.length,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.ingredientsLabel.text = ingredientsText
        
        val stepsText = android.text.SpannableStringBuilder("Steps ")
        stepsText.append("*")
        stepsText.setSpan(
            android.text.style.ForegroundColorSpan(errorColor),
            stepsText.length - 1,
            stepsText.length,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.stepsLabel.text = stepsText
    }

    private fun validateForm(): Boolean {
        val title = binding.titleEditText.text?.toString()?.trim() ?: ""
        val difficulty = binding.difficultySpinner.text?.toString() ?: ""

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a title", Toast.LENGTH_SHORT).show()
            return false
        }

        // Description is now optional
        // Preparation time is now optional

        val cookTime = binding.cookTimeEditText.text?.toString()?.trim() ?: ""
        if (cookTime.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter cooking time", Toast.LENGTH_SHORT).show()
            binding.cookTimeEditText.requestFocus()
            return false
        }

        val cookTimeInt = cookTime.toIntOrNull()
        if (cookTimeInt == null || cookTimeInt <= 0) {
            Toast.makeText(requireContext(), "Please enter a valid cooking time (positive number)", Toast.LENGTH_SHORT).show()
            binding.cookTimeEditText.requestFocus()
            return false
        }

        if (difficulty.isEmpty()) {
            Toast.makeText(requireContext(), "Please select difficulty", Toast.LENGTH_SHORT).show()
            return false
        }

        if (ingredients.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one ingredient", Toast.LENGTH_SHORT).show()
            return false
        }

        if (steps.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one step", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun saveRecipe() {
        val title = binding.titleEditText.text?.toString()?.trim() ?: ""
        val description = binding.descriptionEditText.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val difficultyText = binding.difficultySpinner.text?.toString() ?: "EASY"
        val difficulty = when (difficultyText) {
            "EASY" -> "EASY"
            "MEDIUM" -> "MEDIUM"
            "HARD" -> "HARD"
            else -> "EASY"
        }
        val prepTime = binding.prepTimeEditText.text?.toString()?.trim()?.toIntOrNull()
        val cookTime = binding.cookTimeEditText.text?.toString()?.trim()?.toIntOrNull()
        val servings = binding.servingsEditText.text?.toString()?.trim()?.toIntOrNull()

        if (isEditMode) {
            viewModel.updateRecipe(
                recipeId = recipeId,
                title = title,
                description = description,
                difficulty = difficulty,
                prepTime = prepTime,
                cookTime = cookTime,
                servings = servings,
                ingredients = ingredients,
                steps = steps,
                imagePaths = selectedImagePaths.takeIf { it.isNotEmpty() },
                existingImageUrls = existingImageUrls
            )
        } else {
            viewModel.createRecipe(
                title = title,
                description = description,
                difficulty = difficulty,
                prepTime = prepTime,
                cookTime = cookTime,
                servings = servings,
                ingredients = ingredients,
                steps = steps,
                imagePaths = selectedImagePaths.takeIf { it.isNotEmpty() }
            )
        }
    }
    
    private fun loadRecipeForEdit(recipeId: Long) {
        lifecycleScope.launch {
            recipeRepository.getRecipeById(recipeId)
                .onSuccess { recipe ->
                    binding.titleEditText.setText(recipe.title)
                    binding.descriptionEditText.setText(recipe.description ?: "")
                    
                    val difficultyText = when {
                        recipe.difficulty.equals("EASY", ignoreCase = true) -> "EASY"
                        recipe.difficulty.equals("MEDIUM", ignoreCase = true) -> "MEDIUM"
                        recipe.difficulty.equals("HARD", ignoreCase = true) -> "HARD"
                        else -> "EASY"
                    }
                    binding.difficultySpinner.setText(difficultyText)
                    
                    recipe.prepTime?.let { binding.prepTimeEditText.setText(it.toString()) }
                    recipe.cookTime?.let { binding.cookTimeEditText.setText(it.toString()) }
                    recipe.servings?.let { binding.servingsEditText.setText(it.toString()) }
                    
                    ingredients.clear()
                    ingredients.addAll(recipe.ingredients)
                    ingredientAdapter.notifyDataSetChanged()
                    
                    steps.clear()
                    steps.addAll(recipe.steps)
                    stepAdapter.notifyDataSetChanged()
                    
                    existingImageUrls.clear()
                    val imageUrls: List<String> = recipe.imageUrls ?: (recipe.imageUrl?.let { listOf(it) } ?: emptyList<String>())
                    existingImageUrls.addAll(imageUrls)
                    
                    if (imageUrls.isNotEmpty()) {
                        updateImagePreviewForEdit(imageUrls)
                    } else {
                        binding.placeholderImageView.visibility = View.VISIBLE
                        binding.recipeImagesViewPager.visibility = View.GONE
                    }
                }
                .onFailure { exception ->
                    Toast.makeText(requireContext(), "Unable to load recipe. Please try again.", Toast.LENGTH_LONG).show()
                }
        }
    }
    
    private fun updateImagePreviewForEdit(imageUrls: List<String>) {
        existingImageUrls.clear()
        existingImageUrls.addAll(imageUrls)
        
        updateImagePreview()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.saveRecipeButton.isEnabled = !isLoading
                val buttonText = if (isEditMode) {
                    if (isLoading) "Saving..." else "Save Changes"
                } else {
                    if (isLoading) "Saving..." else "Save Recipe"
                }
                binding.saveRecipeButton.text = buttonText
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recipeCreated.collect { recipe ->
                recipe?.let {
                    val message = if (isEditMode) "Recipe updated successfully" else "Recipe created successfully"
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    } else {
                        @Suppress("DEPRECATION")
                        requireActivity().onBackPressed()
                    }
                    viewModel.clearRecipeCreated()
                }
            }
        }
    }

    private fun getRealPathFromURI(uri: Uri): String? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                copyUriToTempFile(uri)
            } else {
                val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val index = it.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                        if (index >= 0) {
                            val path = it.getString(index)
                            if (path != null && File(path).exists()) {
                                return path
                            }
                        }
                    }
                }
                copyUriToTempFile(uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun copyUriToTempFile(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("recipe_image_", ".jpg", requireContext().cacheDir)
            tempFile.outputStream().use { output ->
                inputStream?.copyTo(output)
            }
            inputStream?.close()
            tempFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
