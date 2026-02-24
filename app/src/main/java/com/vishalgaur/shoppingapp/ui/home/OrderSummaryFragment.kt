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

        orderViewModel.selectedAddressData.observe(viewLifecycleOwner) { address ->
            if (address != null) {
                binding.summaryAddressName.text = "${address.fName} ${address.lName}"
                binding.summaryAddressDetails.text = "${address.streetAddress}, ${address.city}, ${address.countryISOCode}"
            }
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
}
