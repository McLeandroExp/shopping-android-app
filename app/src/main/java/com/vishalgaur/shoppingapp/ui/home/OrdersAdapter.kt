package com.vishalgaur.shoppingapp.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vishalgaur.shoppingapp.R
import com.vishalgaur.shoppingapp.data.UserData
import com.vishalgaur.shoppingapp.databinding.LayoutOrderSummaryCardBinding
import com.vishalgaur.shoppingapp.data.utils.formatToTwoDecimals
import java.time.Month
import java.util.*

class OrdersAdapter(
    ordersList: List<UserData.OrderItem>,
    private val context: Context,
    var proList: List<com.vishalgaur.shoppingapp.data.Product> = emptyList()
) :
    RecyclerView.Adapter<OrdersAdapter.ViewHolder>() {

    lateinit var onClickListener: OnClickListener
    var data: List<UserData.OrderItem> = ordersList

    inner class ViewHolder(private val binding: LayoutOrderSummaryCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(orderData: UserData.OrderItem) {
            binding.orderSummaryCard.setOnClickListener { onClickListener.onCardClick(orderData.orderId) }
            binding.orderSummaryIdTv.text = orderData.orderId
            val calendar = Calendar.getInstance()
            calendar.time = orderData.orderDate
            binding.orderSummaryDateTv.text =
                context.getString(
                    R.string.order_date_text,
                    Month.values()[(calendar.get(Calendar.MONTH))].name,
                    calendar.get(Calendar.DAY_OF_MONTH).toString(),
                    calendar.get(Calendar.YEAR).toString(),
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE)
                )
            binding.orderSummaryStatusValueTv.text = orderData.status
            val totalItems = orderData.items.map { it.quantity }.sum()
            binding.orderSummaryItemsCountTv.text =
                context.getString(R.string.order_items_count_text, totalItems.toString())

            // Build products detail string
            val productsDetail = StringBuilder()
            orderData.items.forEachIndexed { index, cartItem ->
                val product = proList.find { it.productId == cartItem.productId }
                val productName = product?.name ?: "Producto"
                productsDetail.append(context.getString(R.string.order_item_product_detail, productName, cartItem.quantity))
                if (index < orderData.items.size - 1) {
                    productsDetail.append("\n")
                }
            }
            binding.orderSummaryProductsDetailTv.text = productsDetail.toString()

            var totalAmount = 0.0
            orderData.itemsPrices.forEach { (itemId, price) ->
                totalAmount += price * (orderData.items.find { it.itemId == itemId }?.quantity ?: 1)
            }
            totalAmount += orderData.shippingCharges + orderData.importCharges + orderData.taxAmount
            binding.orderSummaryTotalAmountTv.text =
                context.getString(R.string.price_text, totalAmount.formatToTwoDecimals())
        }
    }

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(
			LayoutOrderSummaryCardBinding.inflate(
				LayoutInflater.from(parent.context),
				parent,
				false
			)
		)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.bind(data[position])
	}

	override fun getItemCount() = data.size

	interface OnClickListener {
		fun onCardClick(orderId: String)
	}
}