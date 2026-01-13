package com.chefmate.ui.shopping.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chefmate.R
import com.chefmate.data.api.models.ShoppingListItem

class ShoppingListItemAdapter(
    private val onItemChecked: (ShoppingListItem, Boolean) -> Unit,
    private val onItemDelete: (ShoppingListItem) -> Unit
) : ListAdapter<ShoppingListItem, ShoppingListItemAdapter.ItemViewHolder>(ItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_list, parent, false)
        return ItemViewHolder(view, onItemChecked, onItemDelete)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ItemViewHolder(
        itemView: View,
        private val onItemChecked: (ShoppingListItem, Boolean) -> Unit,
        private val onItemDelete: (ShoppingListItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val nameTextView: TextView = itemView.findViewById(R.id.itemNameTextView)
        private val quantityTextView: TextView = itemView.findViewById(R.id.itemQuantityTextView)
        private val checkBox: CheckBox = itemView.findViewById(R.id.itemCheckBox)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteItemButton)

        fun bind(item: ShoppingListItem) {
            nameTextView.text = item.name
            
            val quantityText = when {
                !item.quantity.isNullOrEmpty() && !item.unit.isNullOrEmpty() -> 
                    "${item.quantity} ${item.unit}"
                !item.quantity.isNullOrEmpty() -> 
                    item.quantity
                else -> ""
            }
            quantityTextView.text = quantityText
            quantityTextView.visibility = if (quantityText.isNotEmpty()) View.VISIBLE else View.GONE

            checkBox.isChecked = item.purchased
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                onItemChecked(item, isChecked)
            }

            deleteButton.setOnClickListener {
                onItemDelete(item)
            }

            // Strike through text if purchased
            if (item.purchased) {
                nameTextView.paintFlags = nameTextView.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                nameTextView.alpha = 0.6f
            } else {
                nameTextView.paintFlags = nameTextView.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                nameTextView.alpha = 1.0f
            }
        }
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<ShoppingListItem>() {
        override fun areItemsTheSame(oldItem: ShoppingListItem, newItem: ShoppingListItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ShoppingListItem, newItem: ShoppingListItem): Boolean {
            return oldItem == newItem
        }
    }
}

