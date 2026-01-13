package com.chefmate.ui.shopping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class ShoppingListFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val textView = TextView(requireContext())
        textView.text = "Списък за пазаруване\n(Ще бъде имплементирано в Фаза 2)"
        textView.textSize = 18f
        textView.setPadding(32, 32, 32, 32)
        return textView
    }
}

