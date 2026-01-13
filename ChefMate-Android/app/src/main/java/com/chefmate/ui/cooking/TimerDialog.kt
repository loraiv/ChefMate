package com.chefmate.ui.cooking

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.chefmate.databinding.DialogTimerBinding
import com.google.android.material.button.MaterialButton

class TimerDialog : DialogFragment() {
    
    private var _binding: DialogTimerBinding? = null
    private val binding get() = _binding!!
    
    var onTimerSet: ((minutes: Int, seconds: Int, label: String?) -> Unit)? = null
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTimerBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Quick preset buttons
        binding.preset1Min.setOnClickListener { setTime(1, 0) }
        binding.preset5Min.setOnClickListener { setTime(5, 0) }
        binding.preset10Min.setOnClickListener { setTime(10, 0) }
        binding.preset15Min.setOnClickListener { setTime(15, 0) }
        binding.preset30Min.setOnClickListener { setTime(30, 0) }
        binding.preset60Min.setOnClickListener { setTime(60, 0) }
        
        // Set timer button
        binding.setTimerButton.setOnClickListener {
            val minutes = binding.minutesInput.text.toString().toIntOrNull() ?: 0
            val seconds = binding.secondsInput.text.toString().toIntOrNull() ?: 0
            val label = binding.labelInput.text.toString().takeIf { it.isNotBlank() }
            
            if (minutes > 0 || seconds > 0) {
                onTimerSet?.invoke(minutes, seconds, label)
                dismiss()
            }
        }
        
        // Cancel button
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }
    
    private fun setTime(minutes: Int, seconds: Int) {
        binding.minutesInput.setText(minutes.toString())
        binding.secondsInput.setText(seconds.toString())
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
