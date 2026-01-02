package com.chefmate.ui.recipes.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chefmate.R
import com.chefmate.data.api.models.RecipeResponse
import com.chefmate.utils.Constants

class RecipeAdapter(
    private val onRecipeClick: (RecipeResponse) -> Unit,
    private val onLikeClick: (RecipeResponse) -> Unit
) : ListAdapter<RecipeResponse, RecipeAdapter.RecipeViewHolder>(RecipeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.recipeTitle)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.recipeDescription)
        private val imageView: ImageView = itemView.findViewById(R.id.recipeImage)
        private val difficultyChip: TextView = itemView.findViewById(R.id.difficultyChip)
        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        private val likeIcon: ImageView = itemView.findViewById(R.id.likeIcon)
        private val likesCountTextView: TextView = itemView.findViewById(R.id.likesCountTextView)

        fun bind(recipe: RecipeResponse) {
            titleTextView.text = recipe.title
            descriptionTextView.text = recipe.description

            // Load image
            if (!recipe.imageUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(recipe.imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(imageView)
            } else {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            // Difficulty
            difficultyChip.text = when (recipe.difficulty) {
                "EASY" -> "ЛЕСНО"
                "MEDIUM" -> "СРЕДНО"
                "HARD" -> "ТРУДНО"
                else -> recipe.difficulty
            }

            // Time
            timeTextView.text = "${recipe.preparationTime} мин"

            // Likes
            likesCountTextView.text = recipe.likesCount.toString()
            likeIcon.setImageResource(
                if (recipe.isLiked) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )

            // Click listeners
            itemView.setOnClickListener { onRecipeClick(recipe) }
            likeIcon.setOnClickListener { onLikeClick(recipe) }
        }
    }

    class RecipeDiffCallback : DiffUtil.ItemCallback<RecipeResponse>() {
        override fun areItemsTheSame(oldItem: RecipeResponse, newItem: RecipeResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RecipeResponse, newItem: RecipeResponse): Boolean {
            return oldItem == newItem
        }
    }
}

