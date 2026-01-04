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
import com.chefmate.R
import com.chefmate.data.repository.RecipeRepository
import com.chefmate.databinding.FragmentRecipeListBinding
import com.chefmate.di.AppModule
import com.bumptech.glide.Glide
import com.chefmate.data.api.ApiClient
import com.chefmate.ui.recipes.adapter.RecipeAdapter
import com.chefmate.ui.recipes.viewmodel.UserRecipesViewModel
import com.chefmate.ui.recipes.viewmodel.UserRecipesViewModelFactory
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.launch

class UserRecipesFragment : Fragment() {

    private var _binding: FragmentRecipeListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserRecipesViewModel by viewModels {
        val tokenManager = TokenManager(requireContext())
        val apiService = AppModule.provideApiService()
        val recipeRepository = RecipeRepository(apiService, tokenManager)
        val userId = arguments?.getLong("userId") ?: -1L
        val username = arguments?.getString("username") ?: ""
        if (userId == -1L) {
            throw IllegalStateException("userId is required for UserRecipesFragment")
        }
        UserRecipesViewModelFactory(recipeRepository, userId, username)
    }

    private lateinit var adapter: RecipeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tokenManager = TokenManager(requireContext())
        if (!tokenManager.isLoggedIn()) {
            findNavController().popBackStack()
            return
        }

        // Validate arguments
        val userId = arguments?.getLong("userId") ?: -1L
        if (userId == -1L) {
            android.util.Log.e("UserRecipesFragment", "Invalid userId: $userId")
            findNavController().popBackStack()
            return
        }

        setupToolbar()
        setupRecyclerView()
        hideSearchAndFilters()
        setupObservers()

        viewModel.loadUserRecipes()
    }

    private fun setupToolbar() {
        val toolbarTitle = binding.root.findViewById<android.widget.TextView>(R.id.toolbarTitle)
        if (toolbarTitle != null) {
            toolbarTitle.text = viewModel.username.ifEmpty { "Рецепти" }
        } else {
            binding.toolbar.title = viewModel.username.ifEmpty { "Рецепти" }
        }
        
        // Load profile image from first recipe if available
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recipes.collect { recipes ->
                if (recipes.isNotEmpty()) {
                    val firstRecipe = recipes.first()
                    val profileImageUrl = firstRecipe.userProfileImageUrl
                    val profileImageView = binding.root.findViewById<android.widget.ImageView>(R.id.userProfileImageToolbar)
                    
                    if (profileImageView != null) {
                        if (!profileImageUrl.isNullOrEmpty() && profileImageUrl != "null") {
                            val imageUrl = if (profileImageUrl.startsWith("http")) {
                                profileImageUrl
                            } else {
                                val baseUrl = ApiClient.BASE_URL.trimEnd('/')
                                val path = if (profileImageUrl.startsWith("/")) profileImageUrl else "/${profileImageUrl}"
                                "$baseUrl$path"
                            }
                            profileImageView.visibility = View.VISIBLE
                            Glide.with(requireContext())
                                .load(imageUrl)
                                .circleCrop()
                                .placeholder(android.R.drawable.ic_menu_myplaces)
                                .error(android.R.drawable.ic_menu_myplaces)
                                .into(profileImageView)
                        } else {
                            profileImageView.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun hideSearchAndFilters() {
        binding.searchInputLayout.visibility = View.GONE
        binding.filtersLayout.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        adapter = RecipeAdapter(
            onRecipeClick = { recipe ->
                navigateToRecipeDetail(recipe.id)
            },
            onLikeClick = { recipe ->
                viewModel.toggleLike(recipe)
            }
        )

        binding.recipesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recipesRecyclerView.adapter = adapter
    }

    private fun setupObservers() {
        var isLoading = false
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                isLoading = loading
                binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                
                // Update empty state when loading changes
                updateEmptyState(viewModel.recipes.value, isLoading)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recipes.collect { recipes ->
                adapter.submitList(recipes)
                updateEmptyState(recipes, isLoading)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    android.util.Log.e("UserRecipesFragment", "Error: $it")
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun updateEmptyState(recipes: List<com.chefmate.data.api.models.RecipeResponse>, isLoading: Boolean) {
        if (recipes.isEmpty() && !isLoading) {
            // Check if viewing own recipes
            val tokenManager = TokenManager(requireContext())
            val currentUserId = tokenManager.getUserId()?.toLongOrNull()
            val isOwnRecipes = currentUserId != null && viewModel.userId == currentUserId
            
            binding.emptyStateTextView.text = when {
                isOwnRecipes -> "Все още нямате създадени рецепти"
                viewModel.username.isNotEmpty() -> "Няма рецепти от ${viewModel.username}"
                else -> "Няма рецепти"
            }
            binding.emptyStateTextView.visibility = View.VISIBLE
        } else {
            binding.emptyStateTextView.visibility = View.GONE
        }
    }

    private fun navigateToRecipeDetail(recipeId: Long) {
        val bundle = Bundle().apply {
            putLong("recipeId", recipeId)
        }
        findNavController().navigate(R.id.action_userRecipesFragment_to_recipeDetailFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

