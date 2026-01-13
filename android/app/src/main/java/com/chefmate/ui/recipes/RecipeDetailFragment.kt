package com.chefmate.ui.recipes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.chefmate.R
import com.chefmate.data.repository.RecipeRepository
import com.chefmate.data.repository.ShoppingRepository
import com.chefmate.databinding.FragmentRecipeDetailBinding
import com.chefmate.di.AppModule
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

    private var recipeId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recipeId = arguments?.getLong("recipeId") ?: -1
    }

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

        if (recipeId == -1L) {
            Toast.makeText(requireContext(), "Грешка: Няма избрана рецепта", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
            return
        }

        setupRecyclerViews()
        setupObservers()
        setupClickListeners()

        viewModel.loadRecipe(recipeId)
    }

    private fun setupRecyclerViews() {
        // Ingredients
        binding.ingredientsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.ingredientsRecyclerView.adapter = IngredientsAdapter()

        // Steps
        binding.stepsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.stepsRecyclerView.adapter = StepsAdapter()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recipe.collect { recipe ->
                recipe?.let { displayRecipe(it) }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // Show/hide loading indicator
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

    private fun displayRecipe(recipe: com.chefmate.data.api.models.RecipeResponse) {
        binding.recipeTitle.text = recipe.title
        binding.recipeDescription.text = recipe.description

        // Image
        if (!recipe.imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(recipe.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(binding.recipeImage)
        }

        // Difficulty
        binding.difficultyChip.text = when (recipe.difficulty) {
            "EASY" -> "ЛЕСНО"
            "MEDIUM" -> "СРЕДНО"
            "HARD" -> "ТРУДНО"
            else -> recipe.difficulty
        }

        // Time
        binding.timeTextView.text = "${recipe.preparationTime} мин"

        // Likes
        binding.likesCountTextView.text = recipe.likesCount.toString()
        binding.likeButton.setImageResource(
            if (recipe.isLiked) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )

        // Comments
        binding.commentsCountTextView.text = "${recipe.commentsCount} коментара"

        // Ingredients
        (binding.ingredientsRecyclerView.adapter as? IngredientsAdapter)?.submitList(
            recipe.ingredients.map { it.name }
        )

        // Steps
        (binding.stepsRecyclerView.adapter as? StepsAdapter)?.submitList(recipe.steps)
    }

    private fun setupClickListeners() {
        binding.likeButton.setOnClickListener {
            viewModel.toggleLike()
        }

        binding.addToShoppingListButton.setOnClickListener {
            viewModel.addToShoppingList()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Simple adapters for ingredients and steps
class IngredientsAdapter : androidx.recyclerview.widget.ListAdapter<String, IngredientsAdapter.ViewHolder>(
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
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        fun bind(ingredient: String) {
            (itemView as android.widget.TextView).text = "• $ingredient"
        }
    }
}

class StepsAdapter : androidx.recyclerview.widget.ListAdapter<String, StepsAdapter.ViewHolder>(
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
        holder.bind(getItem(position), position + 1)
    }

    class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        fun bind(step: String, stepNumber: Int) {
            (itemView as android.widget.TextView).text = "$stepNumber. $step"
        }
    }
}


