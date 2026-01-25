package com.chefmate.ui.recipes.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chefmate.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class RecipeImageAdapter(
    private val imageUrls: List<String>,
    private val isEditMode: Boolean = false,
    private val onDeleteClick: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<RecipeImageAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_image, parent, false)
        return ImageViewHolder(view, isEditMode, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imageUrls[position], position)
    }

    override fun getItemCount(): Int = imageUrls.size

    class ImageViewHolder(
        itemView: View,
        private val isEditMode: Boolean,
        private val onDeleteClick: ((Int) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.recipeImage)
        private val deleteButton: FloatingActionButton = itemView.findViewById(R.id.deleteImageButton)

        fun bind(imageUrl: String, position: Int) {
            if (imageUrl.startsWith("content://") || imageUrl.startsWith("file://")) {
                Glide.with(itemView.context)
                    .load(Uri.parse(imageUrl))
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .fitCenter()
                    .into(imageView)
            } else {
                val fullUrl = if (imageUrl.startsWith("http")) {
                    imageUrl
                } else {
                    val path = if (imageUrl.startsWith("/")) imageUrl else "/$imageUrl"
                    val baseUrl = com.chefmate.data.api.ApiClient.BASE_URL.trimEnd('/')
                    "$baseUrl$path"
                }
                
                Glide.with(itemView.context)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .fitCenter()
                    .into(imageView)
            }

            if (isEditMode && onDeleteClick != null) {
                deleteButton.visibility = View.VISIBLE
                deleteButton.setOnClickListener {
                    onDeleteClick?.invoke(position)
                }
            } else {
                deleteButton.visibility = View.GONE
            }
        }
    }
}

