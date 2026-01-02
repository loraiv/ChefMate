package com.chefmate.ui.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class CookingAssistantFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val textView = TextView(requireContext())
        textView.text = "AI Асистент за готвене\n(Ще бъде имплементирано в Фаза 2)"
        textView.textSize = 18f
        textView.setPadding(32, 32, 32, 32)
        return textView
    }
}

