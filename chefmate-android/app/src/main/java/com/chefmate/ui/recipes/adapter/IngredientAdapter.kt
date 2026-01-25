package com.chefmate.ui.recipes.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chefmate.R
import com.google.android.material.button.MaterialButton

class IngredientAdapter(
    private val ingredients: MutableList<String>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<IngredientAdapter.IngredientViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingredient, parent, false)
        return IngredientViewHolder(view)
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        holder.bind(ingredients[position], position)
    }

    override fun getItemCount(): Int = ingredients.size

    inner class IngredientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(ingredient: String, position: Int) {
            textView.text = ingredient
            itemView.setOnClickListener {
                onRemoveClick(position)
            }
        }
    }
}

