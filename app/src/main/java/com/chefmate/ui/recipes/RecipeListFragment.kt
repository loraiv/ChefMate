package com.chefmate.ui.recipes

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.chefmate.R
import com.chefmate.data.api.models.RecipeResponse
import com.chefmate.data.repository.RecipeRepository
import com.chefmate.databinding.FragmentRecipeListBinding
import com.chefmate.di.AppModule
import com.chefmate.ui.auth.LoginActivity
import com.chefmate.ui.profile.ProfileActivity
import com.chefmate.ui.recipes.adapter.RecipeAdapter
import com.chefmate.ui.recipes.viewmodel.RecipeViewModel
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.launch

class RecipeListFragment : Fragment() {

    private var _binding: FragmentRecipeListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecipeViewModel by viewModels {
        val tokenManager = TokenManager(requireContext())
        val apiService = AppModule.provideApiService()
        val recipeRepository = RecipeRepository(apiService, tokenManager)
        RecipeViewModelFactory(recipeRepository)
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
        setupSearch()
        setupFilters()
        setupObservers()

        viewModel.loadRecipes()
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
            R.id.menu_add_recipe -> {
                navigateToAddRecipe()
                true
            }
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

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                if (query.length >= 2 || query.isEmpty()) {
                    viewModel.searchRecipes(query)
                }
            }
        })
    }

    private fun setupFilters() {
        // Difficulty filter
        binding.filterDifficulty.setOnClickListener {
            showDifficultyDialog()
        }

        // Time filter
        binding.filterTime.setOnClickListener {
            showTimeDialog()
        }
    }

    private fun showDifficultyDialog() {
        val difficulties = arrayOf("Всички", "ЛЕСНО", "СРЕДНО", "ТРУДНО")
        val difficultyValues = arrayOf(null, "EASY", "MEDIUM", "HARD")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Избери трудност")
            .setItems(difficulties) { _, which ->
                val selected = difficultyValues[which]
                viewModel.filterByDifficulty(selected)
                binding.filterDifficulty.text = if (selected == null) "Трудност" else difficulties[which]
            }
            .show()
    }

    private fun showTimeDialog() {
        val timeOptions = arrayOf("Всички", "До 30 мин", "До 60 мин", "До 120 мин")
        val timeValues = arrayOf<Int?>(null, 30, 60, 120)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Избери време")
            .setItems(timeOptions) { _, which ->
                val selected = timeValues[which]
                viewModel.filterByTime(selected)
                binding.filterTime.text = if (selected == null) "Време" else timeOptions[which]
            }
            .show()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recipes.collect { recipes ->
                adapter.submitList(recipes)
                binding.emptyStateTextView.visibility =
                    if (recipes.isEmpty() && !viewModel.isLoading.value) View.VISIBLE
                    else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
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

    private fun navigateToRecipeDetail(recipeId: Long) {
        // Using bundle for now (Safe Args will be generated after build)
        val bundle = Bundle().apply {
            putLong("recipeId", recipeId)
        }
        findNavController().navigate(R.id.action_recipeListFragment_to_recipeDetailFragment, bundle)
    }

    private fun navigateToAddRecipe() {
        findNavController().navigate(R.id.action_recipeListFragment_to_addRecipeFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class RecipeViewModelFactory(
    private val recipeRepository: RecipeRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecipeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecipeViewModel(recipeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

