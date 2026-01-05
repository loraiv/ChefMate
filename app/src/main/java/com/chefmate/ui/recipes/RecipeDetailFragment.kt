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
import com.chefmate.ui.recipes.adapter.CommentAdapter
import com.chefmate.ui.recipes.adapter.IngredientAdapter
import com.chefmate.ui.recipes.adapter.RecipeImageAdapter
import com.chefmate.ui.recipes.adapter.StepAdapter
import com.chefmate.data.api.models.Comment
import com.chefmate.ui.recipes.viewmodel.RecipeDetailViewModel
import com.chefmate.ui.recipes.viewmodel.RecipeDetailViewModelFactory
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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
    private lateinit var commentAdapter: CommentAdapter
    private val comments = mutableListOf<Comment>()
    private var replyingToComment: Comment? = null

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

        setupToolbar()
        setupAdapters()
        setupObservers()
        setupClickListeners()
        setupComments(recipeId)

        viewModel.loadRecipe(recipeId)
    }

    private fun setupToolbar() {
        val toolbar = binding.toolbar
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.let { activity ->
            activity.setSupportActionBar(toolbar)
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            activity.supportActionBar?.setDisplayShowHomeEnabled(true)
        }
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupAdapters() {
        ingredientAdapter = IngredientAdapter(mutableListOf<String>()) { }
        binding.ingredientsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.ingredientsRecyclerView.adapter = ingredientAdapter

        stepAdapter = StepAdapter(mutableListOf<String>()) { }
        binding.stepsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.stepsRecyclerView.adapter = stepAdapter

        val tokenManager = TokenManager(requireContext())
        val currentUserId = tokenManager.getUserId()?.toLongOrNull()
        
        commentAdapter = CommentAdapter(
            comments = comments,
            onReplyClick = { comment ->
                replyingToComment = comment
                binding.commentEditText.hint = "Отговори на ${comment.userName}..."
                binding.commentEditText.requestFocus()
            },
            onLikeClick = { comment ->
                toggleCommentLike(comment)
            },
            onUserClick = { comment ->
                // Validate comment data first
                if (comment.userId <= 0) {
                    android.util.Log.w("RecipeDetailFragment", "Invalid userId in comment: ${comment.userId}")
                    Toast.makeText(requireContext(), "Невалиден потребител", Toast.LENGTH_SHORT).show()
                    return@CommentAdapter
                }
                
                // Post to main thread to ensure view is ready
                view?.post {
                    try {
                        // Check if fragment is still attached
                        if (isAdded && view != null && isResumed) {
                            navigateToUserRecipes(comment.userId, comment.userName)
                        } else {
                            android.util.Log.w("RecipeDetailFragment", "Fragment not ready, cannot navigate. isAdded=$isAdded, view=${view != null}, isResumed=$isResumed")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("RecipeDetailFragment", "Error in onUserClick", e)
                        Toast.makeText(requireContext(), "Грешка при отваряне на профила: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } ?: run {
                    android.util.Log.w("RecipeDetailFragment", "View is null, cannot navigate")
                }
            },
            onDeleteClick = { comment ->
                deleteComment(comment)
            },
            currentUserId = currentUserId
        )
        binding.commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.commentsRecyclerView.adapter = commentAdapter
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

        // Comments count will be updated when comments are loaded

        // Show edit and delete buttons only if user is the owner
        val tokenManager = TokenManager(requireContext())
        val currentUserId = tokenManager.getUserId()?.toLongOrNull()
        if (currentUserId != null && recipe.userId == currentUserId) {
            binding.editRecipeButton?.visibility = View.VISIBLE
            binding.deleteRecipeButton?.visibility = View.VISIBLE
        } else {
            binding.editRecipeButton?.visibility = View.GONE
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

        binding.editRecipeButton?.setOnClickListener {
            viewModel.recipe.value?.let { recipe ->
                navigateToEditRecipe(recipe)
            }
        }

        binding.deleteRecipeButton?.setOnClickListener {
            viewModel.recipe.value?.let { recipe ->
                viewModel.deleteRecipe(recipe.id)
            }
        }

        binding.postCommentButton.setOnClickListener {
            postComment()
        }
    }

    private fun setupComments(recipeId: Long) {
        lifecycleScope.launch {
            try {
                val recipeRepository = RecipeRepository(
                    AppModule.provideApiService(),
                    TokenManager(requireContext())
                )
                recipeRepository.getComments(recipeId)
                    .onSuccess { loadedComments ->
                        try {
                            android.util.Log.d("RecipeDetailFragment", "Loaded ${loadedComments.size} top-level comments")
                            // Log replies for debugging
                            loadedComments.forEach { comment ->
                                val repliesCount = comment.replies?.size ?: 0
                                if (repliesCount > 0) {
                                    android.util.Log.d("RecipeDetailFragment", "Comment ${comment.id} has $repliesCount replies")
                                }
                            }
                            
                            // Use updateComments method which properly updates the adapter
                            commentAdapter.updateComments(loadedComments)
                            comments.clear()
                            comments.addAll(loadedComments)
                            updateCommentsCount(loadedComments.size)
                        } catch (e: Exception) {
                            android.util.Log.e("RecipeDetailFragment", "Error updating comments UI", e)
                            Toast.makeText(requireContext(), "Грешка при показване на коментарите", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .onFailure { error ->
                        android.util.Log.e("RecipeDetailFragment", "Error loading comments", error)
                        Toast.makeText(requireContext(), "Грешка при зареждане на коментарите: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                android.util.Log.e("RecipeDetailFragment", "Unexpected error in setupComments", e)
                Toast.makeText(requireContext(), "Грешка при зареждане на коментарите", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun postComment() {
        val content = binding.commentEditText.text?.toString()?.trim() ?: ""
        if (content.isEmpty()) {
            Toast.makeText(requireContext(), "Моля, въведете коментар", Toast.LENGTH_SHORT).show()
            return
        }

        val recipeId = arguments?.getLong("recipeId") ?: return
        val recipeRepository = RecipeRepository(
            AppModule.provideApiService(),
            TokenManager(requireContext())
        )

        lifecycleScope.launch {
            binding.postCommentButton.isEnabled = false
            binding.postCommentButton.text = "Публикуване..."

            val result = if (replyingToComment != null) {
                // Reply to comment
                recipeRepository.replyToComment(replyingToComment!!.id, content)
            } else {
                // New comment
                recipeRepository.addComment(recipeId, content)
            }

            result.onSuccess { comment ->
                binding.commentEditText.text?.clear()
                val wasReplying = replyingToComment != null
                val parentCommentId = replyingToComment?.id
                replyingToComment = null
                binding.commentInputLayout.hint = "Напиши коментар..."
                binding.commentEditText.hint = "Напиши коментар..."
                
                android.util.Log.d("RecipeDetailFragment", "Comment posted successfully. Was reply: $wasReplying, Parent ID: $parentCommentId")
                
                // Small delay to ensure backend has flushed the transaction and the reply is available
                // Using saveAndFlush in backend ensures immediate persistence
                delay(800)
                
                // Reload comments to show the new reply
                android.util.Log.d("RecipeDetailFragment", "Reloading comments after posting...")
                setupComments(recipeId)
                
                Toast.makeText(requireContext(), "Коментарът е публикуван", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                android.util.Log.e("RecipeDetailFragment", "Error posting comment", error)
                Toast.makeText(requireContext(), "Грешка: ${error.message}", Toast.LENGTH_SHORT).show()
            }

            binding.postCommentButton.isEnabled = true
            binding.postCommentButton.text = "Публикувай"
        }
    }

    private fun toggleCommentLike(comment: Comment) {
        val recipeRepository = RecipeRepository(
            AppModule.provideApiService(),
            TokenManager(requireContext())
        )

        lifecycleScope.launch {
            val result = if (comment.isLiked) {
                recipeRepository.unlikeComment(comment.id)
            } else {
                recipeRepository.likeComment(comment.id)
            }

            result.onSuccess {
                // Reload comments to get updated like status
                val recipeId = arguments?.getLong("recipeId") ?: return@launch
                setupComments(recipeId)
            }.onFailure { error ->
                Toast.makeText(requireContext(), "Грешка: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteComment(comment: Comment) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Изтриване на коментар")
            .setMessage("Сигурни ли сте, че искате да изтриете този коментар?")
            .setPositiveButton("Изтрий") { _, _ ->
                val recipeRepository = RecipeRepository(
                    AppModule.provideApiService(),
                    TokenManager(requireContext())
                )

                lifecycleScope.launch {
                    val result = recipeRepository.deleteComment(comment.id)

                    result.onSuccess {
                        Toast.makeText(requireContext(), "Коментарът е изтрит", Toast.LENGTH_SHORT).show()
                        // Reload comments to update the list
                        val recipeId = arguments?.getLong("recipeId") ?: return@launch
                        setupComments(recipeId)
                    }.onFailure { error ->
                        android.util.Log.e("RecipeDetailFragment", "Error deleting comment", error)
                        Toast.makeText(requireContext(), "Грешка при изтриване: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отказ", null)
            .show()
    }

    private fun updateCommentsCount(count: Int) {
        // Count all comments including replies
        val totalCount = comments.sumOf { comment ->
            1 + (comment.replies?.size ?: 0)
        }
        binding.commentsCountTextView.text = "$totalCount коментара"
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
        try {
            // Validate fragment state - must be added, resumed, and have a view
            if (!isAdded || !isResumed || view == null) {
                android.util.Log.w("RecipeDetailFragment", "Fragment not ready: isAdded=$isAdded, isResumed=$isResumed, view=${view != null}")
                return
            }
            
            // Validate userId before navigating
            if (userId <= 0) {
                android.util.Log.w("RecipeDetailFragment", "Attempted to navigate with invalid userId: $userId")
                Toast.makeText(requireContext(), "Невалиден потребител", Toast.LENGTH_SHORT).show()
                return
            }
            
            val bundle = Bundle().apply {
                putLong("userId", userId)
                putString("username", username.ifEmpty { "Потребител" })
            }
            
            android.util.Log.d("RecipeDetailFragment", "Navigating to user recipes: userId=$userId, username=$username")
            
            // Get nav controller - this should work if fragment is properly attached
            val navController = findNavController()
            navController.navigate(R.id.action_recipeDetailFragment_to_userRecipesFragment, bundle)
            
        } catch (e: IllegalStateException) {
            // Navigation might fail if fragment is not attached or nav controller is not available
            android.util.Log.e("RecipeDetailFragment", "Navigation error (IllegalState): ${e.message}", e)
            android.util.Log.e("RecipeDetailFragment", "Fragment state: isAdded=$isAdded, isResumed=$isResumed, view=${view != null}")
            Toast.makeText(requireContext(), "Грешка при отваряне на профила. Моля опитайте отново.", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            // Invalid arguments
            android.util.Log.e("RecipeDetailFragment", "Navigation error (IllegalArgument): ${e.message}", e)
            Toast.makeText(requireContext(), "Грешка: Невалидни данни за потребителя", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("RecipeDetailFragment", "Error navigating to user recipes", e)
            android.util.Log.e("RecipeDetailFragment", "Exception type: ${e.javaClass.simpleName}, message: ${e.message}")
            e.printStackTrace()
            Toast.makeText(requireContext(), "Грешка при отваряне на профила: ${e.message ?: "Неизвестна грешка"}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToEditRecipe(recipe: com.chefmate.data.api.models.RecipeResponse) {
        val bundle = Bundle().apply {
            putLong("recipeId", recipe.id)
        }
        findNavController().navigate(R.id.action_recipeDetailFragment_to_addRecipeFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
