package com.chefmate.ui.admin.adapter

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chefmate.R
import com.chefmate.data.api.models.UserManagementResponse
import com.google.android.material.card.MaterialCardView

class UserAdapter(
    private val onBlockClick: (UserManagementResponse) -> Unit,
    private val onUnblockClick: (UserManagementResponse) -> Unit,
    private val onDeleteClick: (UserManagementResponse) -> Unit,
    private val onPromoteClick: (UserManagementResponse) -> Unit,
    private val onDemoteClick: (UserManagementResponse) -> Unit,
    private val onViewRecipesClick: (UserManagementResponse) -> Unit,
    private val currentAdminId: Long?
) : ListAdapter<UserManagementResponse, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_admin, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.userCard)
        private val usernameText: TextView = itemView.findViewById(R.id.usernameText)
        private val emailText: TextView = itemView.findViewById(R.id.emailText)
        private val roleText: TextView = itemView.findViewById(R.id.roleText)
        private val statusText: TextView = itemView.findViewById(R.id.statusText)
        private val menuButton: ImageButton = itemView.findViewById(R.id.menuButton)

        fun bind(user: UserManagementResponse) {
            usernameText.text = user.username
            emailText.text = user.email
            roleText.text = "Role: ${user.role}"
            statusText.text = if (user.enabled) "Active" else "Blocked"
            statusText.setTextColor(
                if (user.enabled) {
                    itemView.context.getColor(R.color.success)
                } else {
                    itemView.context.getColor(R.color.error)
                }
            )

            // Make card clickable to view recipes
            cardView.setOnClickListener {
                onViewRecipesClick(user)
            }

            // Setup popup menu
            menuButton.setOnClickListener { view ->
                showPopupMenu(view, user)
            }
        }

        private fun showPopupMenu(view: View, user: UserManagementResponse) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.user_admin_menu, popup.menu)

            val isCurrentUser = user.id == currentAdminId
            val isAdmin = user.role == "ADMIN"

            // Show/hide menu items based on user status
            popup.menu.findItem(R.id.menu_promote_to_admin).isVisible = !isCurrentUser && !isAdmin
            popup.menu.findItem(R.id.menu_demote_from_admin).isVisible = !isCurrentUser && isAdmin
            popup.menu.findItem(R.id.menu_block).isVisible = !isCurrentUser && user.enabled
            popup.menu.findItem(R.id.menu_unblock).isVisible = !isCurrentUser && !user.enabled
            popup.menu.findItem(R.id.menu_delete).isVisible = !isCurrentUser

            popup.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.menu_promote_to_admin -> {
                        onPromoteClick(user)
                        true
                    }
                    R.id.menu_demote_from_admin -> {
                        onDemoteClick(user)
                        true
                    }
                    R.id.menu_block -> {
                        onBlockClick(user)
                        true
                    }
                    R.id.menu_unblock -> {
                        onUnblockClick(user)
                        true
                    }
                    R.id.menu_delete -> {
                        onDeleteClick(user)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<UserManagementResponse>() {
        override fun areItemsTheSame(oldItem: UserManagementResponse, newItem: UserManagementResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UserManagementResponse, newItem: UserManagementResponse): Boolean {
            return oldItem == newItem
        }
    }
}
