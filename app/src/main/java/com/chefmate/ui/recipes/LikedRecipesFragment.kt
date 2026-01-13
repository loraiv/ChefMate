package com.chefmate.ui.recipes

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.chefmate.R
import com.chefmate.data.repository.RecipeRepository
import com.chefmate.databinding.FragmentRecipeListBinding
import com.chefmate.di.AppModule
import com.chefmate.ui.auth.LoginActivity
import com.chefmate.ui.profile.ProfileActivity
import com.chefmate.ui.recipes.adapter.RecipeAdapter
import com.chefmate.ui.recipes.viewmodel.LikedRecipesViewModel
import com.chefmate.ui.recipes.viewmodel.LikedRecipesViewModelFactory
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.launch

class LikedRecipesFragment : Fragment() {

    private var _binding: FragmentRecipeListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LikedRecipesViewModel by viewModels {
        val tokenManager = TokenManager(requireContext())
        val apiService = AppModule.provideApiService()
        val recipeRepository = RecipeRepository(apiService, tokenManager)
        LikedRecipesViewModelFactory(recipeRepository)
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
            return
        }

        setupToolbar()
        setupRecyclerView()
        hideSearchAndFilters()
        setupObservers()

        viewModel.loadLikedRecipes()
    }

    private fun setupToolbar() {
        try {
            val toolbar = binding.toolbar
            val activity = requireActivity()
            if (activity is AppCompatActivity) {
                activity.setSupportActionBar(toolbar)
                setHasOptionsMenu(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        try {
            inflater.inflate(R.menu.toolbar_menu, menu)
            super.onCreateOptionsMenu(menu, inflater)
        } catch (e: Exception) {
            e.printStackTrace()
            super.onCreateOptionsMenu(menu, inflater)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_profile -> {
                navigateToProfile()
                true
            }
            R.id.menu_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun navigateToProfile() {
        val intent = Intent(requireContext(), ProfileActivity::class.java)
        startActivity(intent)
    }

    private fun logout() {
        val tokenManager = TokenManager(requireContext())
        tokenManager.clearToken()
        tokenManager.clearUserInfo()
        tokenManager.clearRememberMeCredentials()
        
        Toast.makeText(requireContext(), "Излязохте успешно", Toast.LENGTH_SHORT).show()
        
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
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
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun updateEmptyState(recipes: List<com.chefmate.data.api.models.RecipeResponse>, isLoading: Boolean) {
        if (recipes.isEmpty() && !isLoading) {
            binding.emptyStateTextView.text = "Нямате запазени рецепти"
            binding.emptyStateTextView.visibility = View.VISIBLE
        } else {
            binding.emptyStateTextView.visibility = View.GONE
        }
    }

    private fun navigateToRecipeDetail(recipeId: Long) {
        val bundle = Bundle().apply {
            putLong("recipeId", recipeId)
        }
        findNavController().navigate(R.id.action_likedRecipesFragment_to_recipeDetailFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

