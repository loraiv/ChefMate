package com.chefmate.ui.recipes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.chefmate.R
import com.chefmate.data.repository.RecipeRepository
import com.chefmate.data.repository.ShoppingRepository
import com.chefmate.databinding.FragmentRecipeDetailBinding
import com.chefmate.di.AppModule
import com.chefmate.ui.recipes.adapter.IngredientAdapter
import com.chefmate.ui.recipes.adapter.RecipeImageAdapter
import com.chefmate.ui.recipes.adapter.StepAdapter
import com.chefmate.ui.recipes.viewmodel.RecipeDetailViewModel
import com.chefmate.ui.recipes.viewmodel.RecipeDetailViewModelFactory
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.launch

class RecipeDetailFragment : Fragment() {

    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecipeDetailViewModel by viewModels {
        val tokenManager = TokenManager(requireContext())
        val apiService = AppModule.provideApiService()
        val recipeRepository = RecipeRepository(apiService, tokenManager)
        val shoppingRepository = ShoppingRepository(apiService, tokenManager)
        RecipeDetailViewModelFactory(recipeRepository, shoppingRepository)
    }

    private lateinit var ingredientAdapter: IngredientAdapter
    private lateinit var stepAdapter: StepAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recipeId = arguments?.getLong("recipeId") ?: return

        setupAdapters()
        setupObservers()
        setupClickListeners()

        viewModel.loadRecipe(recipeId)
    }

    private fun setupAdapters() {
        ingredientAdapter = IngredientAdapter(mutableListOf<String>()) { }
        binding.ingredientsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.ingredientsRecyclerView.adapter = ingredientAdapter

        stepAdapter = StepAdapter(mutableListOf<String>()) { }
        binding.stepsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.stepsRecyclerView.adapter = stepAdapter
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recipe.collect { recipe ->
                recipe?.let { displayRecipe(it) }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // ProgressBar is in the root layout, not in contentCard
                val progressBar = view?.findViewById<android.widget.ProgressBar>(R.id.progressBar)
                progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    // If recipe was deleted, navigate back
                    if (it.contains("изтрита", ignoreCase = true)) {
                        findNavController().popBackStack()
                    }
                    viewModel.clearError()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.shoppingListMessage.collect { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.clearShoppingListMessage()
                }
            }
        }
    }

    private fun displayRecipe(recipe: com.chefmate.data.api.models.RecipeResponse) {
        binding.recipeTitle.text = recipe.title
        binding.recipeDescription.text = recipe.description

        // Load images with ViewPager2 - use imageUrls if available, otherwise fallback to imageUrl
        val imageUrls = if (!recipe.imageUrls.isNullOrEmpty()) {
            recipe.imageUrls
        } else if (!recipe.imageUrl.isNullOrEmpty() && recipe.imageUrl != "null") {
            listOf(recipe.imageUrl)
        } else {
            emptyList()
        }
        setupImagePager(imageUrls)

        // Username and date
        val usernameText = recipe.username ?: "Неизвестен потребител"
        val dateText = try {
            // Handle ISO format: "2024-01-04T15:07:46" or "2024-01-04T15:07:46.123"
            val dateStr = recipe.createdAt.substringBefore(".").substringBefore("+")
            val date = java.time.LocalDateTime.parse(dateStr)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")
            date.format(formatter)
        } catch (e: Exception) {
            recipe.createdAt.substringBefore("T")
        }
        binding.recipeAuthor.text = "От: $usernameText"
        binding.recipeAuthor.setOnClickListener {
            // Navigate to user recipes
            navigateToUserRecipes(recipe.userId, usernameText)
        }
        // Make it look clickable - use primary color
        try {
            val typedValue = android.util.TypedValue()
            requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
            binding.recipeAuthor.setTextColor(typedValue.data)
        } catch (e: Exception) {
            binding.recipeAuthor.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark))
        }
        binding.recipeAuthor.paintFlags = android.graphics.Paint.UNDERLINE_TEXT_FLAG
        
        // User profile image
        val userProfileImageView = binding.root.findViewById<android.widget.ImageView>(R.id.userProfileImage)
        if (userProfileImageView != null) {
            if (!recipe.userProfileImageUrl.isNullOrEmpty() && recipe.userProfileImageUrl != "null") {
                val profileImageUrl = if (recipe.userProfileImageUrl.startsWith("http")) {
                    recipe.userProfileImageUrl
                } else {
                    val baseUrl = com.chefmate.data.api.ApiClient.BASE_URL.trimEnd('/')
                    val path = if (recipe.userProfileImageUrl.startsWith("/")) recipe.userProfileImageUrl else "/${recipe.userProfileImageUrl}"
                    "$baseUrl$path"
                }
                Glide.with(requireContext())
                    .load(profileImageUrl)
                    .circleCrop()
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .error(android.R.drawable.ic_menu_myplaces)
                    .into(userProfileImageView)
            } else {
                userProfileImageView.setImageResource(android.R.drawable.ic_menu_myplaces)
            }
        }
        
        binding.recipeDate.text = "Дата: $dateText"

        val difficultyText = when {
            recipe.difficulty.equals("EASY", ignoreCase = true) || 
            recipe.difficulty.equals("ЛЕСНО", ignoreCase = true) -> "ЛЕСНО"
            recipe.difficulty.equals("MEDIUM", ignoreCase = true) || 
            recipe.difficulty.equals("СРЕДНО", ignoreCase = true) -> "СРЕДНО"
            recipe.difficulty.equals("HARD", ignoreCase = true) || 
            recipe.difficulty.equals("ТРУДНО", ignoreCase = true) -> "ТРУДНО"
            else -> recipe.difficulty
        }
        binding.difficultyChip.text = difficultyText

        // Time
        val time = recipe.totalTime ?: ((recipe.prepTime ?: 0) + (recipe.cookTime ?: 0))
        binding.timeTextView.text = if (time > 0) "⏱ $time мин" else "⏱ -"

        // Ingredients
        ingredientAdapter = IngredientAdapter(recipe.ingredients.toMutableList()) { }
        binding.ingredientsRecyclerView.adapter = ingredientAdapter

        // Steps
        stepAdapter = StepAdapter(recipe.steps.toMutableList()) { }
        binding.stepsRecyclerView.adapter = stepAdapter

        // Likes
        binding.likesCountTextView.text = recipe.likesCount.toString()
        binding.likeButton.setImageResource(
            if (recipe.isLiked) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )

        // Show delete button only if user is the owner
        val tokenManager = TokenManager(requireContext())
        val currentUserId = tokenManager.getUserId()?.toLongOrNull()
        if (currentUserId != null && recipe.userId == currentUserId) {
            binding.deleteRecipeButton?.visibility = View.VISIBLE
        } else {
            binding.deleteRecipeButton?.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.likeButton.setOnClickListener {
            viewModel.recipe.value?.let { recipe ->
                viewModel.toggleLike(recipe)
            }
        }

        binding.addToShoppingListButton.setOnClickListener {
            viewModel.recipe.value?.let { recipe ->
                viewModel.addToShoppingList(recipe.id)
            }
        }

        binding.deleteRecipeButton?.setOnClickListener {
            viewModel.recipe.value?.let { recipe ->
                viewModel.deleteRecipe(recipe.id)
            }
        }
    }

    private fun setupImagePager(imageUrls: List<String>) {
        if (imageUrls.isNotEmpty()) {
            val adapter = RecipeImageAdapter(imageUrls)
            binding.recipeImagesViewPager.adapter = adapter
            
            // Only show indicators if more than one image
            if (imageUrls.size > 1) {
                setupImageIndicators(imageUrls.size)
                binding.recipeImagesViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        updateImageIndicators(position, imageUrls.size)
                    }
                })
            } else {
                binding.imageIndicatorLayout.visibility = View.GONE
            }
        } else {
            binding.recipeImagesViewPager.visibility = View.GONE
            binding.imageIndicatorLayout.visibility = View.GONE
        }
    }

    private fun setupImageIndicators(count: Int) {
        binding.imageIndicatorLayout.removeAllViews()
        if (count <= 1) {
            binding.imageIndicatorLayout.visibility = View.GONE
            return
        }
        binding.imageIndicatorLayout.visibility = View.VISIBLE
        
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
    }

    private fun updateImageIndicators(selectedPosition: Int, count: Int) {
        for (i in 0 until binding.imageIndicatorLayout.childCount) {
            val dot = binding.imageIndicatorLayout.getChildAt(i)
            val drawable = dot.background as? android.graphics.drawable.GradientDrawable
            drawable?.setColor(if (i == selectedPosition) 0xFF6200EE.toInt() else 0xFFCCCCCC.toInt())
        }
    }

    private fun navigateToUserRecipes(userId: Long, username: String) {
        val bundle = Bundle().apply {
            putLong("userId", userId)
            putString("username", username)
        }
        findNavController().navigate(R.id.action_recipeDetailFragment_to_userRecipesFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
