package com.chefmate.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.chefmate.R
import com.chefmate.databinding.FragmentAdminBinding
import com.chefmate.ui.admin.adapter.UserAdapter
import com.chefmate.utils.TokenManager
import kotlinx.coroutines.launch

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AdminViewModel by viewModels()
    private lateinit var tokenManager: TokenManager
    private lateinit var userAdapter: UserAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        tokenManager = TokenManager(requireContext())
        
        if (!tokenManager.isAdmin()) {
            Toast.makeText(requireContext(), "Access denied. Administrator privileges are required to access this section.", Toast.LENGTH_LONG).show()
            requireActivity().onBackPressed()
            return
        }
        
        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        viewModel.loadUsers()
    }

    private fun setupToolbar() {
        val toolbar = binding.toolbar
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.let { activity ->
            activity.setSupportActionBar(toolbar)
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            activity.supportActionBar?.setDisplayShowHomeEnabled(true)
            activity.supportActionBar?.title = "Admin Panel"
        }
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        val currentAdminId = tokenManager.getUserId()?.toLongOrNull()
        
        userAdapter = UserAdapter(
            onBlockClick = { user ->
                viewModel.blockUser(user.id)
            },
            onUnblockClick = { user ->
                viewModel.unblockUser(user.id)
            },
            onDeleteClick = { user ->
                viewModel.deleteUser(user.id)
            },
            onPromoteClick = { user ->
                viewModel.promoteToAdmin(user.id)
            },
            onDemoteClick = { user ->
                viewModel.demoteFromAdmin(user.id)
            },
            onViewRecipesClick = { user ->
                navigateToUserRecipes(user.id, user.username)
            },
            currentAdminId = currentAdminId
        )
        
        binding.usersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.usersRecyclerView.adapter = userAdapter
    }

    private fun navigateToUserRecipes(userId: Long, username: String) {
        val bundle = Bundle().apply {
            putLong("userId", userId)
            putString("username", username)
            putBoolean("isAdminView", true) // Flag to indicate admin is viewing
        }
        findNavController().navigate(R.id.action_adminFragment_to_userRecipesFragment, bundle)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.users.collect { users ->
                userAdapter.submitList(users)
                binding.emptyStateTextView.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
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
            viewModel.successMessage.collect { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.clearSuccessMessage()
                    viewModel.loadUsers() // Refresh list
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadUsers()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
