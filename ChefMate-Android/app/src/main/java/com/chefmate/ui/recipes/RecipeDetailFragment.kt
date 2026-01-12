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
                binding.commentEditText.hint = "Reply to ${comment.userName}..."
                binding.commentEditText.requestFocus()
            },
            onLikeClick = { comment ->
                toggleCommentLike(comment)
            },
            onUserClick = { comment ->
                if (comment.userId <= 0) {
                    android.util.Log.w("RecipeDetailFragment", "Invalid userId in comment: ${comment.userId}")
                    Toast.makeText(requireContext(), "Invalid user", Toast.LENGTH_SHORT).show()
                    return@CommentAdapter
                }
                
                view?.post {
                    try {
                        if (isAdded && view != null && isResumed) {
                            navigateToUserRecipes(comment.userId, comment.userName)
                        } else {
                            android.util.Log.w("RecipeDetailFragment", "Fragment not ready, cannot navigate. isAdded=$isAdded, view=${view != null}, isResumed=$isResumed")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("RecipeDetailFragment", "Error in onUserClick", e)
                        Toast.makeText(requireContext(), "Error opening profile: ${e.message}", Toast.LENGTH_SHORT).show()
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
                val progressBar = view?.findViewById<android.widget.ProgressBar>(R.id.progressBar)
                progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    if (it.contains("deleted", ignoreCase = true)) {
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

        val imageUrls = if (!recipe.imageUrls.isNullOrEmpty()) {
            recipe.imageUrls
        } else if (!recipe.imageUrl.isNullOrEmpty() && recipe.imageUrl != "null") {
            listOf(recipe.imageUrl)
        } else {
            emptyList()
        }
        setupImagePager(imageUrls)

        val usernameText = recipe.username ?: "Unknown user"
        val dateText = try {
            val dateStr = recipe.createdAt.substringBefore(".").substringBefore("+")
            val date = java.time.LocalDateTime.parse(dateStr)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")
            date.format(formatter)
        } catch (e: Exception) {
            recipe.createdAt.substringBefore("T")
        }
        binding.recipeAuthor.text = "By: $usernameText"
        binding.recipeAuthor.setOnClickListener {
            navigateToUserRecipes(recipe.userId, usernameText)
        }
        try {
            val typedValue = android.util.TypedValue()
            requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
            binding.recipeAuthor.setTextColor(typedValue.data)
        } catch (e: Exception) {
            binding.recipeAuthor.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark))
        }
        binding.recipeAuthor.paintFlags = android.graphics.Paint.UNDERLINE_TEXT_FLAG
        
        val userProfileImageView = binding.root.findViewById<android.widget.ImageView>(R.id.userProfileImage)
        if (userProfileImageView != null) {
            Glide.with(requireContext()).clear(userProfileImageView)
            
            val profileImageUrl = recipe.userProfileImageUrl
            if (!profileImageUrl.isNullOrEmpty() && profileImageUrl != "null" && profileImageUrl.trim().isNotEmpty()) {
                val fullImageUrl = if (profileImageUrl.startsWith("http")) {
                    profileImageUrl
                } else {
                    val baseUrl = com.chefmate.data.api.ApiClient.BASE_URL.trimEnd('/')
                    val path = if (profileImageUrl.startsWith("/")) profileImageUrl else "/${profileImageUrl}"
                    "$baseUrl$path"
                }
                
                val urlWithTimestamp = "$fullImageUrl?t=${System.currentTimeMillis()}"
                
                Glide.with(requireContext())
                    .load(urlWithTimestamp)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                    .circleCrop()
                    .placeholder(R.drawable.ic_user_placeholder_gray)
                    .error(R.drawable.ic_user_placeholder_gray)
                    .fallback(R.drawable.ic_user_placeholder_gray)
                    .into(userProfileImageView)
            } else {
                userProfileImageView.setImageResource(R.drawable.ic_user_placeholder_gray)
            }
        }
        
        binding.recipeDate.text = "Date: $dateText"

        val difficultyText = when {
            recipe.difficulty.equals("EASY", ignoreCase = true) -> "EASY"
            recipe.difficulty.equals("MEDIUM", ignoreCase = true) -> "MEDIUM"
            recipe.difficulty.equals("HARD", ignoreCase = true) -> "HARD"
            else -> recipe.difficulty
        }
        binding.difficultyChip.text = difficultyText

        // Time
        val time = recipe.totalTime ?: ((recipe.prepTime ?: 0) + (recipe.cookTime ?: 0))
        binding.timeTextView.text = if (time > 0) "⏱ $time min" else "⏱ -"

        // Ingredients
        ingredientAdapter = IngredientAdapter(recipe.ingredients.toMutableList()) { }
        binding.ingredientsRecyclerView.adapter = ingredientAdapter

        // Steps
        stepAdapter = StepAdapter(recipe.steps.toMutableList()) { }
        binding.stepsRecyclerView.adapter = stepAdapter

        // Likes
        binding.likesCountTextView.text = recipe.likesCount.toString()
        binding.likeButton.setImageResource(
            if (recipe.isLiked) R.drawable.ic_star_filled
            else R.drawable.ic_star_outline
        )

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

        binding.startCookingButton.setOnClickListener {
            viewModel.recipe.value?.let { recipe ->
                navigateToCookingMode(recipe.id)
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
                            loadedComments.forEach { comment ->
                                val repliesCount = comment.replies?.size ?: 0
                                if (repliesCount > 0) {
                                    android.util.Log.d("RecipeDetailFragment", "Comment ${comment.id} has $repliesCount replies")
                                }
                            }
                            
                            commentAdapter.updateComments(loadedComments)
                            comments.clear()
                            comments.addAll(loadedComments)
                            updateCommentsCount(loadedComments.size)
                        } catch (e: Exception) {
                            android.util.Log.e("RecipeDetailFragment", "Error updating comments UI", e)
                            Toast.makeText(requireContext(), "Error displaying comments", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .onFailure { error ->
                        android.util.Log.e("RecipeDetailFragment", "Error loading comments", error)
                        Toast.makeText(requireContext(), "Error loading comments: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                android.util.Log.e("RecipeDetailFragment", "Unexpected error in setupComments", e)
                Toast.makeText(requireContext(), "Error loading comments", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun postComment() {
        val content = binding.commentEditText.text?.toString()?.trim() ?: ""
        if (content.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a comment", Toast.LENGTH_SHORT).show()
            return
        }

        val recipeId = arguments?.getLong("recipeId") ?: return
        val recipeRepository = RecipeRepository(
            AppModule.provideApiService(),
            TokenManager(requireContext())
        )

        lifecycleScope.launch {
            binding.postCommentButton.isEnabled = false
            binding.postCommentButton.text = "Publishing..."

            val result = if (replyingToComment != null) {
                recipeRepository.replyToComment(replyingToComment!!.id, content)
            } else {
                recipeRepository.addComment(recipeId, content)
            }

            result.onSuccess { comment ->
                binding.commentEditText.text?.clear()
                val wasReplying = replyingToComment != null
                val parentCommentId = replyingToComment?.id
                replyingToComment = null
                binding.commentInputLayout.hint = "Write a comment..."
                binding.commentEditText.hint = "Write a comment..."
                
                android.util.Log.d("RecipeDetailFragment", "Comment posted successfully. Was reply: $wasReplying, Parent ID: $parentCommentId")
                
                delay(800)
                
                android.util.Log.d("RecipeDetailFragment", "Reloading comments after posting...")
                setupComments(recipeId)
                
                Toast.makeText(requireContext(), "Comment published", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                android.util.Log.e("RecipeDetailFragment", "Error posting comment", error)
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }

            binding.postCommentButton.isEnabled = true
            binding.postCommentButton.text = "Post"
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
                val recipeId = arguments?.getLong("recipeId") ?: return@launch
                setupComments(recipeId)
            }.onFailure { error ->
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteComment(comment: Comment) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Comment")
            .setMessage("Are you sure you want to delete this comment?")
            .setPositiveButton("Delete") { _, _ ->
                val recipeRepository = RecipeRepository(
                    AppModule.provideApiService(),
                    TokenManager(requireContext())
                )

                lifecycleScope.launch {
                    val result = recipeRepository.deleteComment(comment.id)

                    result.onSuccess {
                        Toast.makeText(requireContext(), "Comment deleted", Toast.LENGTH_SHORT).show()
                        val recipeId = arguments?.getLong("recipeId") ?: return@launch
                        setupComments(recipeId)
                    }.onFailure { error ->
                        android.util.Log.e("RecipeDetailFragment", "Error deleting comment", error)
                        Toast.makeText(requireContext(), "Error deleting: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateCommentsCount(count: Int) {
        val totalCount = comments.sumOf { comment ->
            1 + (comment.replies?.size ?: 0)
        }
        binding.commentsCountTextView.text = "$totalCount comments"
    }

    private fun setupImagePager(imageUrls: List<String>) {
        if (imageUrls.isNotEmpty()) {
            val adapter = RecipeImageAdapter(imageUrls)
            binding.recipeImagesViewPager.adapter = adapter
            
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

    private fun navigateToCookingMode(recipeId: Long) {
        try {
            if (!isAdded || !isResumed || view == null) {
                android.util.Log.w("RecipeDetailFragment", "Fragment not ready for navigation")
                return
            }
            
            if (recipeId <= 0) {
                android.util.Log.w("RecipeDetailFragment", "Invalid recipeId: $recipeId")
                Toast.makeText(requireContext(), "Invalid recipe", Toast.LENGTH_SHORT).show()
                return
            }
            
            val bundle = Bundle().apply {
                putLong("recipeId", recipeId)
            }
            
            findNavController().navigate(R.id.action_recipeDetailFragment_to_cookingModeFragment, bundle)
        } catch (e: Exception) {
            android.util.Log.e("RecipeDetailFragment", "Error navigating to cooking mode", e)
            Toast.makeText(requireContext(), "Navigation error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToUserRecipes(userId: Long, username: String) {
        try {
            if (!isAdded || !isResumed || view == null) {
                android.util.Log.w("RecipeDetailFragment", "Fragment not ready: isAdded=$isAdded, isResumed=$isResumed, view=${view != null}")
                return
            }
            
            if (userId <= 0) {
                android.util.Log.w("RecipeDetailFragment", "Attempted to navigate with invalid userId: $userId")
                Toast.makeText(requireContext(), "Invalid user", Toast.LENGTH_SHORT).show()
                return
            }
            
            val bundle = Bundle().apply {
                putLong("userId", userId)
                putString("username", username.ifEmpty { "User" })
            }
            
            android.util.Log.d("RecipeDetailFragment", "Navigating to user recipes: userId=$userId, username=$username")
            
            val navController = findNavController()
            navController.navigate(R.id.action_recipeDetailFragment_to_userRecipesFragment, bundle)
            
        } catch (e: IllegalStateException) {
            android.util.Log.e("RecipeDetailFragment", "Navigation error (IllegalState): ${e.message}", e)
            android.util.Log.e("RecipeDetailFragment", "Fragment state: isAdded=$isAdded, isResumed=$isResumed, view=${view != null}")
            Toast.makeText(requireContext(), "Error opening profile. Please try again.", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            android.util.Log.e("RecipeDetailFragment", "Navigation error (IllegalArgument): ${e.message}", e)
            Toast.makeText(requireContext(), "Error: Invalid user data", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("RecipeDetailFragment", "Error navigating to user recipes", e)
            android.util.Log.e("RecipeDetailFragment", "Exception type: ${e.javaClass.simpleName}, message: ${e.message}")
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error opening profile: ${e.message ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
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
