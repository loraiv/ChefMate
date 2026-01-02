package com.chefmate.ui.recipes

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chefmate.R
import com.chefmate.data.api.models.Ingredient
import com.chefmate.data.repository.RecipeRepository
import com.chefmate.databinding.FragmentAddRecipeBinding
import com.chefmate.di.AppModule
import com.chefmate.ui.recipes.viewmodel.AddRecipeViewModel
import com.chefmate.ui.recipes.viewmodel.AddRecipeViewModelFactory
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.launch
import java.io.File

class AddRecipeFragment : Fragment() {

    private var _binding: FragmentAddRecipeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddRecipeViewModel by viewModels {
        val tokenManager = TokenManager(requireContext())
        val apiService = AppModule.provideApiService()
        val recipeRepository = RecipeRepository(apiService, tokenManager)
        AddRecipeViewModelFactory(recipeRepository)
    }

    private var selectedImagePath: String? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            imageUri?.let {
                selectedImagePath = getRealPathFromURI(it)
                binding.recipeImagePreview.setImageURI(it)
            }
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

        setupRecyclerViews()
        setupClickListeners()
        setupObservers()
    }

    private fun setupRecyclerViews() {
        // Ingredients
        binding.ingredientsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.ingredientsRecyclerView.adapter = EditableIngredientsAdapter(
            onRemove = { ingredient ->
                viewModel.removeIngredient(ingredient)
            }
        )

        // Steps
        binding.stepsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.stepsRecyclerView.adapter = EditableStepsAdapter(
            onRemove = { step ->
                viewModel.removeStep(step)
            }
        )
    }

    private fun setupClickListeners() {
        binding.selectImageButton.setOnClickListener {
            openImagePicker()
        }

        binding.difficultySpinner.setOnClickListener {
            showDifficultyDialog()
        }

        binding.addIngredientButton.setOnClickListener {
            showAddIngredientDialog()
        }

        binding.addStepButton.setOnClickListener {
            showAddStepDialog()
        }

        binding.saveRecipeButton.setOnClickListener {
            saveRecipe()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.ingredients.collect { ingredients ->
                (binding.ingredientsRecyclerView.adapter as? EditableIngredientsAdapter)?.submitList(ingredients)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.steps.collect { steps ->
                (binding.stepsRecyclerView.adapter as? EditableStepsAdapter)?.submitList(steps)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedDifficulty.collect { difficulty ->
                binding.difficultySpinner.setText(
                    when (difficulty) {
                        "EASY" -> "ЛЕСНО"
                        "MEDIUM" -> "СРЕДНО"
                        "HARD" -> "ТРУДНО"
                        else -> "Избери трудност"
                    }
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.saveRecipeButton.isEnabled = !isLoading
                binding.saveRecipeButton.text = if (isLoading) "Запазване..." else "Запази рецепта"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recipeSaved.collect { saved ->
                if (saved) {
                    Toast.makeText(requireContext(), "Рецептата е запазена успешно!", Toast.LENGTH_SHORT).show()
                    // Navigate back to recipe list
                    androidx.navigation.fragment.findNavController(this@AddRecipeFragment)
                        .navigate(com.chefmate.R.id.action_addRecipe_to_recipeList)
                }
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun getRealPathFromURI(uri: Uri): String? {
        // For Android 10+ use different approach
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().cacheDir, "recipe_image_${System.currentTimeMillis()}.jpg")
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun showDifficultyDialog() {
        val difficulties = arrayOf("ЛЕСНО", "СРЕДНО", "ТРУДНО")
        val difficultyValues = arrayOf("EASY", "MEDIUM", "HARD")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Избери трудност")
            .setItems(difficulties) { _, which ->
                viewModel.setDifficulty(difficultyValues[which])
            }
            .show()
    }

    private fun showAddIngredientDialog() {
        val input = android.widget.EditText(requireContext())
        input.hint = "Име на продукта"

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Добави продукт")
            .setView(input)
            .setPositiveButton("Добави") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.addIngredient(Ingredient(name = name, amount = ""))
                }
            }
            .setNegativeButton("Отказ", null)
            .show()
    }

    private fun showAddStepDialog() {
        val input = android.widget.EditText(requireContext())
        input.hint = "Описание на стъпката"
        input.minLines = 3

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Добави стъпка")
            .setView(input)
            .setPositiveButton("Добави") { _, _ ->
                val step = input.text.toString().trim()
                if (step.isNotEmpty()) {
                    viewModel.addStep(step)
                }
            }
            .setNegativeButton("Отказ", null)
            .show()
    }

    private fun saveRecipe() {
        val title = binding.titleEditText.text?.toString()?.trim()
        val description = binding.descriptionEditText.text?.toString()?.trim()
        val prepTimeText = binding.prepTimeEditText.text?.toString()?.trim()

        if (title.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Моля, въведете заглавие", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Моля, въведете описание", Toast.LENGTH_SHORT).show()
            return
        }

        val prepTime = prepTimeText?.toIntOrNull() ?: 0
        if (prepTime <= 0) {
            Toast.makeText(requireContext(), "Моля, въведете валидно време", Toast.LENGTH_SHORT).show()
            return
        }

        if (viewModel.ingredients.value.isEmpty()) {
            Toast.makeText(requireContext(), "Моля, добавете поне един продукт", Toast.LENGTH_SHORT).show()
            return
        }

        if (viewModel.steps.value.isEmpty()) {
            Toast.makeText(requireContext(), "Моля, добавете поне една стъпка", Toast.LENGTH_SHORT).show()
            return
        }

        if (viewModel.selectedDifficulty.value == null) {
            Toast.makeText(requireContext(), "Моля, изберете трудност", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.saveRecipe(
            title = title,
            description = description,
            prepTime = prepTime,
            imagePath = selectedImagePath
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Adapters for editable lists
class EditableIngredientsAdapter(
    private val onRemove: (Ingredient) -> Unit
) : androidx.recyclerview.widget.ListAdapter<Ingredient, EditableIngredientsAdapter.ViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Ingredient>() {
        override fun areItemsTheSame(oldItem: Ingredient, newItem: Ingredient) = oldItem.name == newItem.name
        override fun areContentsTheSame(oldItem: Ingredient, newItem: Ingredient) = oldItem == newItem
    }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onRemove)
    }

    class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        fun bind(ingredient: Ingredient, onRemove: (Ingredient) -> Unit) {
            (itemView as android.widget.TextView).text = "• ${ingredient.name}"
            itemView.setOnLongClickListener {
                onRemove(ingredient)
                true
            }
        }
    }
}

class EditableStepsAdapter(
    private val onRemove: (String) -> Unit
) : androidx.recyclerview.widget.ListAdapter<String, EditableStepsAdapter.ViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1, onRemove)
    }

    class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        fun bind(step: String, stepNumber: Int, onRemove: (String) -> Unit) {
            (itemView as android.widget.TextView).text = "$stepNumber. $step"
            itemView.setOnLongClickListener {
                onRemove(step)
                true
            }
        }
    }
}


