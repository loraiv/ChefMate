package com.chefmate.ui.recipes.adapter

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chefmate.R
import com.chefmate.data.api.models.RecipeResponse

class RecipeAdapter(
    private val onRecipeClick: (RecipeResponse) -> Unit,
    private val onLikeClick: (RecipeResponse) -> Unit,
    private val onDeleteClick: ((RecipeResponse) -> Unit)? = null,
    private val isAdminView: Boolean = false
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
        private val adminMenuButton: ImageButton? = itemView.findViewById(R.id.adminMenuButton)

        fun bind(recipe: RecipeResponse) {
            titleTextView.text = recipe.title
            descriptionTextView.text = recipe.description

            // Load image
            android.util.Log.d("RecipeAdapter", "Recipe imageUrl from API: ${recipe.imageUrl}")
            if (!recipe.imageUrl.isNullOrEmpty() && recipe.imageUrl != "null") {
                val imageUrl = if (recipe.imageUrl.startsWith("http")) {
                    recipe.imageUrl
                } else {
                    // Ensure URL starts with /
                    val path = if (recipe.imageUrl.startsWith("/")) recipe.imageUrl else "/${recipe.imageUrl}"
                    val baseUrl = com.chefmate.data.api.ApiClient.BASE_URL.trimEnd('/')
                    val fullUrl = "$baseUrl$path"
                    android.util.Log.d("RecipeAdapter", "Constructed image URL: $fullUrl (base: $baseUrl, path: $path)")
                    fullUrl
                }
                android.util.Log.d("RecipeAdapter", "Loading image from: $imageUrl")
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .into(imageView)
            } else {
                android.util.Log.d("RecipeAdapter", "Recipe has no image URL or it's null/empty")
                imageView.setImageResource(R.drawable.ic_image_placeholder)
            }

            difficultyChip.text = when {
                recipe.difficulty.equals("EASY", ignoreCase = true) -> "EASY"
                recipe.difficulty.equals("MEDIUM", ignoreCase = true) -> "MEDIUM"
                recipe.difficulty.equals("HARD", ignoreCase = true) -> "HARD"
                else -> recipe.difficulty
            }

            // Time
            val time = recipe.totalTime ?: ((recipe.prepTime ?: 0) + (recipe.cookTime ?: 0))
            timeTextView.text = if (time > 0) "⏱ $time min" else "⏱ -"

            // Likes
            likesCountTextView.text = recipe.likesCount.toString()
            likeIcon.setImageResource(
                if (recipe.isLiked) R.drawable.ic_star_filled
                else R.drawable.ic_star_outline
            )

            // Click listeners
            itemView.setOnClickListener { onRecipeClick(recipe) }
            likeIcon.setOnClickListener { onLikeClick(recipe) }

            // Admin menu button
            if (isAdminView && adminMenuButton != null && onDeleteClick != null) {
                adminMenuButton.visibility = View.VISIBLE
                adminMenuButton.setOnClickListener { view ->
                    showAdminMenu(view, recipe)
                }
            } else {
                adminMenuButton?.visibility = View.GONE
            }
        }

        private fun showAdminMenu(view: View, recipe: RecipeResponse) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.recipe_admin_menu, popup.menu)

            popup.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.menu_delete_recipe -> {
                        onDeleteClick?.invoke(recipe)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
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

