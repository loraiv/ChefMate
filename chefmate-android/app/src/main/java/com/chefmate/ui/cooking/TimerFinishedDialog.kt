package com.chefmate.ui.cooking

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.chefmate.databinding.DialogTimerFinishedBinding

class TimerFinishedDialog : DialogFragment() {
    
    private var _binding: DialogTimerFinishedBinding? = null
    private val binding get() = _binding!!
    
    var onStopAlarm: (() -> Unit)? = null
    var onDismissCallback: (() -> Unit)? = null
    
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
        _binding = DialogTimerFinishedBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val label = arguments?.getString("label")
        if (label != null) {
            binding.timerLabelText.text = label
        }
        
        binding.stopButton.setOnClickListener {
            onStopAlarm?.invoke()
            dismiss()
        }
    }
    
    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        onDismissCallback?.invoke()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance(label: String?): TimerFinishedDialog {
            return TimerFinishedDialog().apply {
                arguments = Bundle().apply {
                    putString("label", label)
                }
            }
        }
    }
}
