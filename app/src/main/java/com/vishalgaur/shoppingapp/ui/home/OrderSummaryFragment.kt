package com.vishalgaur.shoppingapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.vishalgaur.shoppingapp.R
import com.vishalgaur.shoppingapp.data.utils.formatToTwoDecimals
import com.vishalgaur.shoppingapp.databinding.FragmentOrderSummaryBinding
import com.vishalgaur.shoppingapp.viewModels.OrderViewModel

class OrderSummaryFragment : Fragment() {

    private lateinit var binding: FragmentOrderSummaryBinding
    private val orderViewModel: OrderViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOrderSummaryBinding.inflate(inflater, container, false)
        setupUI()
        return binding.root
    }

    private fun setupUI() {
        binding.orderSummaryAppBar.topAppBar.title = "Resumen de Pedido"
        binding.orderSummaryAppBar.topAppBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val productsAdapter = SummaryProductsAdapter()
        binding.summaryProductsRv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.summaryProductsRv.adapter = productsAdapter

        orderViewModel.selectedAddressData.observe(viewLifecycleOwner) { address ->
            if (address != null) {
                binding.summaryAddressName.text = "${address.fName} ${address.lName}"
                binding.summaryAddressDetails.text = "${address.streetAddress}, ${address.city}, ${address.countryISOCode}"
            }
        }
        
        orderViewModel.cartItems.observe(viewLifecycleOwner) { items ->
            val products = orderViewModel.cartProducts.value ?: emptyList()
            val prices = orderViewModel.priceList.value ?: emptyMap()
            productsAdapter.setData(items, products, prices)
        }

        orderViewModel.cartProducts.observe(viewLifecycleOwner) { products ->
            val items = orderViewModel.cartItems.value ?: emptyList()
            val prices = orderViewModel.priceList.value ?: emptyMap()
            productsAdapter.setData(items, products, prices)
        }
        
        orderViewModel.shippingCharges.observe(viewLifecycleOwner) { displayPrices() }
        orderViewModel.importCharges.observe(viewLifecycleOwner) { displayPrices() }
        orderViewModel.taxAmount.observe(viewLifecycleOwner) { displayPrices() }
        orderViewModel.totalWithCharges.observe(viewLifecycleOwner) { displayPrices() }

        binding.orderSummaryNextBtn.setOnClickListener {
            findNavController().navigate(R.id.action_orderSummaryFragment_to_selectPaymentFragment)
        }
    }

    private fun displayPrices() {
        val subtotal = orderViewModel.getItemsPriceTotal()
        val shipping = orderViewModel.shippingCharges.value ?: 0.0
        val import = orderViewModel.importCharges.value ?: 0.0
        val tax = orderViewModel.taxAmount.value ?: 0.0
        val total = orderViewModel.totalWithCharges.value ?: subtotal
        
        binding.priceCard.priceItemsLabelTv.text = "Items (${orderViewModel.getItemsCount()})"
        binding.priceCard.priceItemsAmountTv.text = getString(R.string.price_text, subtotal.formatToTwoDecimals())
        binding.priceCard.priceShippingAmountTv.text = getString(R.string.price_text, shipping.formatToTwoDecimals())
        binding.priceCard.priceChargesAmountTv.text = getString(R.string.price_text, import.formatToTwoDecimals())
        binding.priceCard.priceTaxAmountTv.text = getString(R.string.price_text, tax.formatToTwoDecimals())
        binding.priceCard.priceTotalAmountTv.text = getString(R.string.price_text, total.formatToTwoDecimals())
    }

    inner class SummaryProductsAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<SummaryProductsAdapter.ViewHolder>() {
        private var items: List<com.vishalgaur.shoppingapp.data.UserData.CartItem> = emptyList()
        private var products: List<com.vishalgaur.shoppingapp.data.Product> = emptyList()
        private var prices: Map<String, Double> = emptyMap()

        fun setData(newItems: List<com.vishalgaur.shoppingapp.data.UserData.CartItem>, newProducts: List<com.vishalgaur.shoppingapp.data.Product>, newPrices: Map<String, Double>) {
            items = newItems
            products = newProducts
            prices = newPrices
            notifyDataSetChanged()
        }

        inner class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val nameTv: android.widget.TextView = view.findViewById(R.id.summary_item_name)
            val priceTv: android.widget.TextView = view.findViewById(R.id.summary_item_price)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_summary_product, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val product = products.find { it.productId == item.productId }
            val unitPrice = prices[item.itemId] ?: product?.price ?: 0.0
            val subtotal = unitPrice * item.quantity
            
            holder.nameTv.text = "${product?.name ?: "Producto"} x${item.quantity}"
            holder.priceTv.text = getString(R.string.price_text, subtotal.formatToTwoDecimals())
        }

        override fun getItemCount() = items.size
    }
}
