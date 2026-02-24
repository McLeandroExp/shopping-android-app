package com.vishalgaur.shoppingapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vishalgaur.shoppingapp.data.UserData
import com.vishalgaur.shoppingapp.databinding.FragmentAdminUsersBinding
import com.vishalgaur.shoppingapp.viewModels.HomeViewModel

class AdminUsersFragment : Fragment() {

    private lateinit var binding: FragmentAdminUsersBinding
    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var adapter: UserAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdminUsersBinding.inflate(inflater, container, false)
        
        setupUI()
        setupAdapter()
        observeViewModel()
        
        viewModel.fetchAllUsers()
        
        return binding.root
    }

    private fun setupUI() {
        binding.adminUsersTopAppBar.topAppBar.title = "Administrar Usuarios"
        binding.adminUsersTopAppBar.topAppBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupAdapter() {
        adapter = UserAdapter(emptyList(), object : UserAdapter.OnClickListener {
            override fun onDeleteClick(user: UserData) {
                showDeleteDialog(user)
            }
        })
        binding.adminUsersRecyclerView.adapter = adapter
    }

    private fun showDeleteDialog(user: UserData) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Usuario")
            .setMessage("¿Estás seguro de que deseas eliminar permanentemente al usuario '${user.name}'? Si es un vendedor, TODOS sus productos serán eliminados. Esta acción es irreversible.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteUserAccount(user.userId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observeViewModel() {
        val sessionManager = com.vishalgaur.shoppingapp.data.ShoppingAppSessionManager(requireContext())
        val currentUserId = sessionManager.getUserIdFromSession()
        
        viewModel.allUsers.observe(viewLifecycleOwner) { users ->
            val filteredUsers = users.filter { it.userId != currentUserId }
            adapter.dataChanged(filteredUsers)
        }
        
        viewModel.dataStatus.observe(viewLifecycleOwner) { status ->
            if (status == com.vishalgaur.shoppingapp.data.utils.StoreDataStatus.LOADING) {
                binding.loaderLayout.loaderFrameLayout.visibility = View.VISIBLE
            } else {
                binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
            }
        }
    }
}
