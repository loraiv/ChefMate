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

    private lateinit var viewModel: UserRecipesViewModel
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

        val userId = arguments?.getLong("userId") ?: -1L
        val username = arguments?.getString("username") ?: ""
        
        if (userId <= 0) {
            android.util.Log.e("UserRecipesFragment", "Invalid userId: $userId")
            Toast.makeText(requireContext(), "Invalid user", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        val apiService = AppModule.provideApiService()
        val recipeRepository = RecipeRepository(apiService, tokenManager)
        val viewModelFactory = UserRecipesViewModelFactory(recipeRepository, userId, username)
        viewModel = androidx.lifecycle.ViewModelProvider(this, viewModelFactory)[UserRecipesViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        hideSearchAndFilters()
        setupObservers()

        viewModel.loadUserRecipes()
    }

    private fun setupToolbar() {
        val toolbar = binding.toolbar
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.let { activity ->
            activity.setSupportActionBar(toolbar)
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            activity.supportActionBar?.setDisplayShowHomeEnabled(true)
            activity.supportActionBar?.title = "" // Clear app name
        }
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        val toolbarTitle = binding.root.findViewById<android.widget.TextView>(R.id.toolbarTitle)
        if (toolbarTitle != null) {
            toolbarTitle.text = viewModel.username.ifEmpty { "Recipes" }
        } else {
            binding.toolbar.title = viewModel.username.ifEmpty { "Recipes" }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recipes.collect { recipes ->
                if (recipes.isNotEmpty()) {
                    val firstRecipe = recipes.first()
                    val profileImageUrl = firstRecipe.userProfileImageUrl
                    val profileImageView = binding.root.findViewById<android.widget.ImageView>(R.id.userProfileImageToolbar)
                    
                    if (profileImageView != null) {
                        Glide.with(requireContext()).clear(profileImageView)
                        
                        if (!profileImageUrl.isNullOrEmpty() && profileImageUrl != "null" && profileImageUrl.trim().isNotEmpty()) {
                            val imageUrl = if (profileImageUrl.startsWith("http")) {
                                profileImageUrl
                            } else {
                                val baseUrl = ApiClient.BASE_URL.trimEnd('/')
                                val path = if (profileImageUrl.startsWith("/")) profileImageUrl else "/${profileImageUrl}"
                                "$baseUrl$path"
                            }
                            
                            val urlWithTimestamp = "$imageUrl?t=${System.currentTimeMillis()}"
                            
                            profileImageView.visibility = View.VISIBLE
                            Glide.with(requireContext())
                                .load(urlWithTimestamp)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                                .circleCrop()
                                .placeholder(R.drawable.ic_user_placeholder_gray)
                                .error(R.drawable.ic_user_placeholder_gray)
                                .into(profileImageView)
                        } else {
                            profileImageView.setImageResource(R.drawable.ic_user_placeholder_gray)
                            profileImageView.visibility = View.VISIBLE
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
            val tokenManager = TokenManager(requireContext())
            val currentUserId = tokenManager.getUserId()?.toLongOrNull()
            val isOwnRecipes = currentUserId != null && viewModel.userId == currentUserId
            
            binding.emptyStateTextView.text = when {
                isOwnRecipes -> "You haven't created any recipes yet"
                viewModel.username.isNotEmpty() -> "No recipes from ${viewModel.username}"
                else -> "No recipes"
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

