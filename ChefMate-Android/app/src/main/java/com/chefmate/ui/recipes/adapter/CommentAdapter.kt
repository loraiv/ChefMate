package com.chefmate.ui.recipes.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chefmate.R
import com.chefmate.data.api.models.Comment
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(
    private val comments: MutableList<Comment>,
    private val onReplyClick: (Comment) -> Unit,
    private val onLikeClick: (Comment) -> Unit,
    private val onUserClick: (Comment) -> Unit,
    private val onDeleteClick: ((Comment) -> Unit)? = null,
    private val currentUserId: Long? = null,
    private val depth: Int = 0,
    private val isAdmin: Boolean = false
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view, onReplyClick, onLikeClick, onUserClick, onDeleteClick, currentUserId, depth, isAdmin)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount(): Int = comments.size

    fun updateComments(newComments: List<Comment>) {
        comments.clear()
        comments.addAll(newComments)
        notifyDataSetChanged()
    }
    
    fun refreshRepliesForComment(commentId: Long, newReplies: List<Comment>) {
        val commentIndex = comments.indexOfFirst { it.id == commentId }
        if (commentIndex >= 0) {
            val comment = comments[commentIndex]
            val updatedComment = comment.copy(replies = newReplies)
            comments[commentIndex] = updatedComment
            notifyItemChanged(commentIndex)
        }
    }

    fun addComment(comment: Comment) {
        comments.add(0, comment)
        notifyItemInserted(0)
    }

    fun updateComment(updatedComment: Comment) {
        val index = comments.indexOfFirst { it.id == updatedComment.id }
        if (index >= 0) {
            comments[index] = updatedComment
            notifyItemChanged(index)
        }
    }

    class CommentViewHolder(
        itemView: View,
        private val onReplyClick: (Comment) -> Unit,
        private val onLikeClick: (Comment) -> Unit,
        private val onUserClick: (Comment) -> Unit,
        private val onDeleteClick: ((Comment) -> Unit)?,
        private val currentUserId: Long?,
        private val depth: Int = 0,
        private val isAdmin: Boolean = false
    ) : RecyclerView.ViewHolder(itemView) {
        private val authorTextView: TextView = itemView.findViewById(R.id.commentAuthor)
        private val dateTextView: TextView = itemView.findViewById(R.id.commentDate)
        private val contentTextView: TextView = itemView.findViewById(R.id.commentContent)
        private val likeButton: ImageButton = itemView.findViewById(R.id.likeCommentButton)
        private val likesCountTextView: TextView = itemView.findViewById(R.id.commentLikesCount)
        private val replyButton: TextView = itemView.findViewById(R.id.replyButton)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteCommentButton)
        private val repliesContainer: ViewGroup? = itemView.findViewById(R.id.repliesContainer)
        private val repliesRecyclerView: RecyclerView = itemView.findViewById(R.id.repliesRecyclerView)
        private val userProfileImageView: ImageView = itemView.findViewById(R.id.commentUserProfileImage)

        fun bind(comment: Comment) {
            if (depth > 0) {
                val cardView = itemView as? com.google.android.material.card.MaterialCardView
                cardView?.let {
                    val margin = (8 - depth * 2).coerceAtLeast(2)
                    val layoutParams = it.layoutParams as? ViewGroup.MarginLayoutParams
                    layoutParams?.setMargins(margin, margin, margin, margin)
                    it.layoutParams = layoutParams
                }
                
                val layoutParams = userProfileImageView.layoutParams
                val size = (32 - depth * 4).coerceAtLeast(24)
                layoutParams.width = size
                layoutParams.height = size
                userProfileImageView.layoutParams = layoutParams
                
                if (depth > 1) {
                    authorTextView.textSize = 12f
                    contentTextView.textSize = 12f
                }
            }
            
            val username = comment.userName.ifEmpty { "Unknown user" }
            authorTextView.text = username.lowercase()
            contentTextView.text = comment.content.ifEmpty { "(empty comment)" }

            Glide.with(itemView.context).clear(userProfileImageView)
            
            if (!comment.userProfileImageUrl.isNullOrEmpty() && comment.userProfileImageUrl != "null") {
                val profileImageUrl = if (comment.userProfileImageUrl.startsWith("http")) {
                    comment.userProfileImageUrl
                } else {
                    val baseUrl = com.chefmate.data.api.ApiClient.BASE_URL.trimEnd('/')
                    val path = if (comment.userProfileImageUrl.startsWith("/")) comment.userProfileImageUrl else "/${comment.userProfileImageUrl}"
                    "$baseUrl$path"
                }
                Glide.with(itemView.context)
                    .load(profileImageUrl)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                    .circleCrop()
                    .placeholder(R.drawable.ic_user_placeholder)
                    .error(R.drawable.ic_user_placeholder)
                    .into(userProfileImageView)
            } else {
                userProfileImageView.setImageResource(R.drawable.ic_user_placeholder)
            }

            if (comment.userId > 0) {
                val userClickListener = View.OnClickListener {
                    onUserClick(comment)
                }
                authorTextView.setOnClickListener(userClickListener)
                userProfileImageView.setOnClickListener(userClickListener)
                // Make it visually clickable
                authorTextView.isClickable = true
                userProfileImageView.isClickable = true
            } else {
                authorTextView.isClickable = false
                userProfileImageView.isClickable = false
            }

            // Format date
            val dateText = try {
                val dateStr = comment.createdAt.substringBefore(".").substringBefore("+")
                val date = java.time.LocalDateTime.parse(dateStr)
                val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                date.format(formatter)
            } catch (e: Exception) {
                comment.createdAt.substringBefore("T")
            }
            dateTextView.text = dateText

            // Likes - use heart icon
            likesCountTextView.text = comment.likesCount.toString()
            
            // Hide like and reply buttons for admins
            if (isAdmin) {
                likeButton.visibility = View.GONE
                likesCountTextView.visibility = View.GONE
                replyButton.visibility = View.GONE
            } else {
                likeButton.visibility = View.VISIBLE
                likesCountTextView.visibility = View.VISIBLE
                likeButton.setImageResource(
                    if (comment.isLiked) R.drawable.ic_heart_filled
                    else R.drawable.ic_heart
                )
                likeButton.setOnClickListener {
                    onLikeClick(comment)
                }

                replyButton.visibility = View.VISIBLE
                replyButton.setOnClickListener {
                    onReplyClick(comment)
                }
            }

            val isOwnComment = currentUserId != null && comment.userId == currentUserId
            if ((isOwnComment || isAdmin) && onDeleteClick != null) {
                deleteButton.visibility = View.VISIBLE
                deleteButton.setOnClickListener {
                    onDeleteClick?.invoke(comment)
                }
            } else {
                deleteButton.visibility = View.GONE
            }

            val replies = comment.replies ?: emptyList()
            android.util.Log.d("CommentAdapter", "Binding comment ${comment.id}, replies count: ${replies.size}")
            if (replies.isNotEmpty()) {
                repliesContainer?.visibility = View.VISIBLE
                android.util.Log.d("CommentAdapter", "Showing ${replies.size} replies for comment ${comment.id}")
                val existingAdapter = repliesRecyclerView.adapter as? CommentAdapter
                if (existingAdapter != null) {
                    android.util.Log.d("CommentAdapter", "Updating existing adapter for comment ${comment.id}")
                    existingAdapter.updateComments(replies)
                } else {
                    android.util.Log.d("CommentAdapter", "Creating new adapter for comment ${comment.id}")
                    val repliesAdapter = CommentAdapter(
                        replies.toMutableList(),
                        onReplyClick,
                        onLikeClick,
                        onUserClick,
                        onDeleteClick,
                        currentUserId,
                        depth + 1,
                        isAdmin
                    )
                    repliesRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
                    repliesRecyclerView.adapter = repliesAdapter
                }
            } else {
                repliesContainer?.visibility = View.GONE
                android.util.Log.d("CommentAdapter", "No replies for comment ${comment.id}, hiding container")
            }
        }
    }
}

