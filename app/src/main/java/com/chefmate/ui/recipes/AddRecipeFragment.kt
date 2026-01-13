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
                    Toast.makeText(requireContext(), "Можете да добавите максимум 5 снимки", Toast.LENGTH_SHORT).show()
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
                "Разрешението за камерата е необходимо за снимане на снимки",
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
                    Toast.makeText(requireContext(), "Можете да добавите максимум 5 снимки", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "Грешка при обработка на снимката", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(requireContext(), "Грешка при обработка на снимката", Toast.LENGTH_SHORT).show()
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR && result.data != null) {
            val cropError = UCrop.getError(result.data!!)
            Toast.makeText(requireContext(), "Грешка при обработка на снимката: ${cropError?.message}", Toast.LENGTH_SHORT).show()
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
            activity.supportActionBar?.title = if (isEditMode) "Редактирай рецепта" else "Добави рецепта"
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
        val difficulties = arrayOf("ЛЕСНО", "СРЕДНО", "ТРУДНО")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, difficulties)
        
        binding.difficultySpinner.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Избери трудност")
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
                Toast.makeText(requireContext(), "Можете да добавите максимум 5 снимки", Toast.LENGTH_SHORT).show()
            } else {
                showImageSourceDialog()
            }
        }
        
        if (!isEditMode) {
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
    
    private fun updateImagePreview() {
        val allImageUrls = if (isEditMode) {
            existingImageUrls + selectedImageUris.map { it.toString() }
        } else {
            selectedImageUris.map { it.toString() }
        }
        
        val adapter = RecipeImageAdapter(
            imageUrls = allImageUrls,
            isEditMode = isEditMode,
            onDeleteClick = { position ->
                handleImageDelete(position)
            }
        )
        binding.recipeImagesViewPager.adapter = adapter
        
        if (allImageUrls.size > 1) {
            binding.imageIndicatorLayout.visibility = View.VISIBLE
            setupImageIndicators(allImageUrls.size)
        } else {
            binding.imageIndicatorLayout.visibility = View.GONE
        }
        
        binding.selectImageButton.text = "Добави снимка (${allImageUrls.size}/5)"
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
        val options = arrayOf("Галерия", "Камера")
        AlertDialog.Builder(requireContext())
            .setTitle("Избери източник на снимка")
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
            Toast.makeText(requireContext(), "Грешка при отваряне на камерата: ${e.message}", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), "Грешка при четене на снимката", Toast.LENGTH_SHORT).show()
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
                com.yalantis.ucrop.model.AspectRatio("Оригинална", 0f, 0f)
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
            Toast.makeText(requireContext(), "Грешка при обработка на снимката: ${e.message}", Toast.LENGTH_SHORT).show()
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
        input.hint = "Например: 500г брашно"

        AlertDialog.Builder(requireContext())
            .setTitle("Добави продукт")
            .setView(input)
            .setPositiveButton("Добави") { _, _ ->
                val ingredient = input.text.toString().trim()
                if (ingredient.isNotEmpty()) {
                    ingredients.add(ingredient)
                    ingredientAdapter.notifyItemInserted(ingredients.size - 1)
                }
            }
            .setNegativeButton("Отказ", null)
            .show()
    }

    private fun showAddStepDialog() {
        val input = android.widget.EditText(requireContext())
        input.hint = "Опиши стъпката..."
        input.minLines = 3
        input.maxLines = 5

        AlertDialog.Builder(requireContext())
            .setTitle("Добави стъпка")
            .setView(input)
            .setPositiveButton("Добави") { _, _ ->
                val step = input.text.toString().trim()
                if (step.isNotEmpty()) {
                    steps.add(step)
                    stepAdapter.notifyItemInserted(steps.size - 1)
                }
            }
            .setNegativeButton("Отказ", null)
            .show()
    }

    private fun setupSaveButton() {
        if (isEditMode) {
            binding.saveRecipeButton.text = "Запази промените"
        }
        binding.saveRecipeButton.setOnClickListener {
            if (validateForm()) {
                saveRecipe()
            }
        }
    }

    private fun validateForm(): Boolean {
        val title = binding.titleEditText.text?.toString()?.trim() ?: ""
        val description = binding.descriptionEditText.text?.toString()?.trim() ?: ""
        val difficulty = binding.difficultySpinner.text?.toString() ?: ""

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Моля, въведете заглавие", Toast.LENGTH_SHORT).show()
            return false
        }

        if (description.isEmpty()) {
            Toast.makeText(requireContext(), "Моля, въведете описание", Toast.LENGTH_SHORT).show()
            return false
        }

        if (difficulty.isEmpty()) {
            Toast.makeText(requireContext(), "Моля, изберете трудност", Toast.LENGTH_SHORT).show()
            return false
        }

        if (ingredients.isEmpty()) {
            Toast.makeText(requireContext(), "Моля, добавете поне един продукт", Toast.LENGTH_SHORT).show()
            return false
        }

        if (steps.isEmpty()) {
            Toast.makeText(requireContext(), "Моля, добавете поне една стъпка", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun saveRecipe() {
        val title = binding.titleEditText.text?.toString()?.trim() ?: ""
        val description = binding.descriptionEditText.text?.toString()?.trim() ?: ""
        val difficultyText = binding.difficultySpinner.text?.toString() ?: "ЛЕСНО"
        val difficulty = when (difficultyText) {
            "ЛЕСНО" -> "EASY"
            "СРЕДНО" -> "MEDIUM"
            "ТРУДНО" -> "HARD"
            else -> "EASY"
        }
        val prepTime = binding.prepTimeEditText.text?.toString()?.toIntOrNull()
        val cookTime = null

        if (isEditMode) {
            viewModel.updateRecipe(
                recipeId = recipeId,
                title = title,
                description = description,
                difficulty = difficulty,
                prepTime = prepTime,
                cookTime = cookTime,
                servings = null,
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
                servings = null,
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
                    binding.descriptionEditText.setText(recipe.description)
                    
                    val difficultyText = when {
                        recipe.difficulty.equals("EASY", ignoreCase = true) || 
                        recipe.difficulty.equals("ЛЕСНО", ignoreCase = true) -> "ЛЕСНО"
                        recipe.difficulty.equals("MEDIUM", ignoreCase = true) || 
                        recipe.difficulty.equals("СРЕДНО", ignoreCase = true) -> "СРЕДНО"
                        recipe.difficulty.equals("HARD", ignoreCase = true) || 
                        recipe.difficulty.equals("ТРУДНО", ignoreCase = true) -> "ТРУДНО"
                        else -> "ЛЕСНО"
                    }
                    binding.difficultySpinner.setText(difficultyText)
                    
                    recipe.prepTime?.let { binding.prepTimeEditText.setText(it.toString()) }
                    
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
                    }
                }
                .onFailure { exception ->
                    Toast.makeText(requireContext(), "Грешка при зареждане на рецепта: ${exception.message}", Toast.LENGTH_LONG).show()
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
                    if (isLoading) "Запазване..." else "Запази промените"
                } else {
                    if (isLoading) "Запазване..." else "Запази рецепта"
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
                    val message = if (isEditMode) "Рецептата е обновена успешно!" else "Рецептата е създадена успешно!"
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
