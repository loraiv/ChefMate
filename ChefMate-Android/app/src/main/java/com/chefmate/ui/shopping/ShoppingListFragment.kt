package com.chefmate.ui.shopping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.chefmate.R
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chefmate.data.repository.ShoppingRepository
import com.chefmate.databinding.FragmentShoppingListBinding
import com.chefmate.di.AppModule
import com.chefmate.ui.shopping.adapter.ShoppingListItemAdapter
import com.chefmate.ui.shopping.viewmodel.ShoppingListViewModel
import com.chefmate.ui.shopping.viewmodel.ShoppingListViewModelFactory
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.launch

class ShoppingListFragment : Fragment() {

    private var _binding: FragmentShoppingListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShoppingListViewModel by viewModels {
        val tokenManager = TokenManager(requireContext())
        val apiService = AppModule.provideApiService()
        val shoppingRepository = ShoppingRepository(apiService, tokenManager)
        ShoppingListViewModelFactory(shoppingRepository)
    }

    private lateinit var adapter: ShoppingListItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShoppingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        viewModel.loadShoppingList()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadShoppingList()
    }

    private fun setupRecyclerView() {
        adapter = ShoppingListItemAdapter(
            onItemChecked = { item, checked ->
                viewModel.updateItem(item, checked)
            },
            onItemDelete = { item ->
                viewModel.deleteItem(item)
            }
        )
        binding.shoppingListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.shoppingListRecyclerView.adapter = adapter
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.shoppingList.collect { shoppingList ->
                shoppingList?.let {
                    // Always show "My Shopping List" regardless of the name in database
                    binding.listNameTextView.text = "My Shopping List"
                    adapter.submitList(it.items)
                    updateEmptyState(it.items.isEmpty() && !viewModel.isLoading.value)
                } ?: run {
                    binding.listNameTextView.text = "My Shopping List"
                    adapter.submitList(emptyList())
                    updateEmptyState(true)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                if (!isLoading) {
                    viewModel.shoppingList.value?.let {
                        updateEmptyState(it.items.isEmpty())
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.message.collect { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.clearMessage()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.addProductButton.setOnClickListener {
            showAddProductDialog()
        }
    }

    private fun showAddProductDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_shopping_item, null)
        val nameEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.productNameEditText)
        val quantityEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.quantityEditText)
        val unitEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.unitEditText)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Add Product")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameEditText.text?.toString()?.trim() ?: ""
                if (name.isNotEmpty()) {
                    val quantity = quantityEditText.text?.toString()?.trim() ?: ""
                    val unit = unitEditText.text?.toString()?.trim() ?: ""
                    viewModel.addItem(name, quantity, unit)
                } else {
                    Toast.makeText(requireContext(), "Please enter product name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateTextView.text = "You haven't added any products yet"
            binding.emptyStateTextView.visibility = View.VISIBLE
            binding.shoppingListRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateTextView.visibility = View.GONE
            binding.shoppingListRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
