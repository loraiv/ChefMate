package com.chefmate.ui.ai.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chefmate.R
import com.chefmate.ui.ai.viewmodel.ChatMessage
import com.google.android.material.card.MaterialCardView

class ChatAdapter(
    private val messages: List<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userMessageCard: MaterialCardView = itemView.findViewById(R.id.userMessageCard)
        private val userMessageText: TextView = itemView.findViewById(R.id.userMessageText)
        private val aiMessageCard: MaterialCardView = itemView.findViewById(R.id.aiMessageCard)
        private val aiMessageText: TextView = itemView.findViewById(R.id.aiMessageText)

        fun bind(message: ChatMessage) {
            if (message.isUser) {
                // Show user message
                userMessageCard.visibility = View.VISIBLE
                aiMessageCard.visibility = View.GONE
                userMessageText.text = message.message
            } else {
                // Show AI message
                userMessageCard.visibility = View.GONE
                aiMessageCard.visibility = View.VISIBLE
                aiMessageText.text = message.message
            }
        }
    }
}

