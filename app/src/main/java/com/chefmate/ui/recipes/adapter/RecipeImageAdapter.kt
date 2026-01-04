package com.chefmate.ui.recipes.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chefmate.R

class RecipeImageAdapter(private val imageUrls: List<String>) : RecyclerView.Adapter<RecipeImageAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imageUrls[position])
    }

    override fun getItemCount(): Int = imageUrls.size

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.recipeImage)

        fun bind(imageUrl: String) {
            // Check if it's a URI string (starts with content:// or file://)
            if (imageUrl.startsWith("content://") || imageUrl.startsWith("file://")) {
                Glide.with(itemView.context)
                    .load(Uri.parse(imageUrl))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(imageView)
            } else {
                // It's a URL
                val fullUrl = if (imageUrl.startsWith("http")) {
                    imageUrl
                } else {
                    val path = if (imageUrl.startsWith("/")) imageUrl else "/$imageUrl"
                    val baseUrl = com.chefmate.data.api.ApiClient.BASE_URL.trimEnd('/')
                    "$baseUrl$path"
                }
                
                Glide.with(itemView.context)
                    .load(fullUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(imageView)
            }
        }
    }
}

