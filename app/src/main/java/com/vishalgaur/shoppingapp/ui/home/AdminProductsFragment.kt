package com.vishalgaur.shoppingapp.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vishalgaur.shoppingapp.R
import com.vishalgaur.shoppingapp.data.Product
import com.vishalgaur.shoppingapp.databinding.FragmentAdminProductsBinding
import com.vishalgaur.shoppingapp.viewModels.HomeViewModel

class AdminProductsFragment : Fragment() {

    private lateinit var binding: FragmentAdminProductsBinding
    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var adapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdminProductsBinding.inflate(inflater, container, false)
        
        setupUI()
        setupAdapter()
        observeViewModel()
        
        return binding.root
    }

    private fun setupUI() {
        binding.adminTopAppBar.topAppBar.title = "Administrar Productos"
        binding.adminTopAppBar.topAppBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupAdapter() {
        adapter = ProductAdapter(emptyList(), emptyList(), requireContext(), isAdminManagement = true)
        adapter.onClickListener = object : ProductAdapter.OnClickListener {
            override fun onClick(productData: Product) {
                // Navigate to details if needed
            }

            override fun onDeleteClick(productData: Product) {
                showDeleteDialog(productData)
            }
        }
        binding.adminProductsRecyclerView.adapter = adapter
    }

    private fun showDeleteDialog(product: Product) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Producto")
            .setMessage("¿Estás seguro de que deseas eliminar permanentemente el producto '${product.name}'? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteProduct(product.productId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.allProducts.observe(viewLifecycleOwner) { products ->
            if (products != null) {
                adapter.data = products
                adapter.notifyDataSetChanged()
            }
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
