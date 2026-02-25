package com.vishalgaur.shoppingapp.ui.home

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vishalgaur.shoppingapp.R
import com.vishalgaur.shoppingapp.data.Product
import com.vishalgaur.shoppingapp.data.ShoppingAppSessionManager
import com.vishalgaur.shoppingapp.databinding.ProductsListItemBinding
import com.vishalgaur.shoppingapp.getOfferPercentage
import com.vishalgaur.shoppingapp.data.utils.formatToTwoDecimals

class ProductAdapter(
	proList: List<Product>,
	userLikes: List<String>,
	private val context: Context,
	private val isMyProducts: Boolean = false,
	private val isAdminManagement: Boolean = false
) :
	RecyclerView.Adapter<ProductAdapter.ItemViewHolder>() {

	var data = proList
	var likesList = userLikes

	lateinit var onClickListener: OnClickListener
	lateinit var bindImageButtons: BindImageButtons
	private val sessionManager = ShoppingAppSessionManager(context)

	inner class ItemViewHolder(binding: ProductsListItemBinding) :
		RecyclerView.ViewHolder(binding.root) {
		private val proName = binding.productNameTv
		private val proPrice = binding.productPriceTv
		private val productCard = binding.productCard
		private val productImage = binding.productImageView
		private val proDeleteButton = binding.productDeleteButton
		private val proEditBtn = binding.productEditButton
		private val proMrp = binding.productActualPriceTv
		private val proOffer = binding.productOfferValueTv
		private val proRatingBar = binding.productRatingBar
		private val proLikeButton = binding.productLikeCheckbox
		private val proCartButton = binding.productAddToCartButton

		fun bind(productData: Product) {
			productCard.setOnClickListener {
				onClickListener.onClick(productData)
			}
			proName.text = productData.name
			proPrice.text =
				context.getString(R.string.pro_details_price_value, productData.price.formatToTwoDecimals())
			proRatingBar.rating = productData.rating.toFloat()
			proMrp.visibility = View.GONE
			proOffer.visibility = View.GONE
			if (productData.images.isNotEmpty()) {
				val imgUrl = productData.images[0].toUri().buildUpon().scheme("https").build()
				Glide.with(context)
					.asBitmap()
					.load(imgUrl)
					.into(productImage)

				productImage.clipToOutline = true
			}

			proLikeButton.isChecked = likesList.contains(productData.productId)

			if (sessionManager.isUserSeller() || sessionManager.isUserAdmin()) {
				proLikeButton.visibility = View.GONE
				proCartButton.visibility = View.GONE
				
				if (sessionManager.isUserAdmin()) {
					if (isAdminManagement) {
						proEditBtn.visibility = View.GONE
						proDeleteButton.visibility = View.VISIBLE
						proDeleteButton.setOnClickListener {
							onClickListener.onDeleteClick(productData)
						}
					} else {
						proEditBtn.visibility = View.GONE
						proDeleteButton.visibility = View.GONE
					}
				} else if (isMyProducts) {
					proEditBtn.visibility = View.VISIBLE
					proDeleteButton.visibility = View.VISIBLE
					proEditBtn.setOnClickListener {
						onClickListener.onEditClick(productData.productId)
					}
					proDeleteButton.setOnClickListener {
						onClickListener.onDeleteClick(productData)
					}
				} else {
					proEditBtn.visibility = View.GONE
					proDeleteButton.visibility = View.GONE
				}
			} else {
				proEditBtn.visibility = View.GONE
				proDeleteButton.visibility = View.GONE
				bindImageButtons.setLikeButton(productData.productId, proLikeButton)
				bindImageButtons.setCartButton(productData.productId, proCartButton)
				proLikeButton.setOnCheckedChangeListener { _, _ ->


				}
				proLikeButton.setOnClickListener {
					onClickListener.onLikeClick(productData.productId)
				}
				proCartButton.setOnClickListener {
					onClickListener.onAddToCartClick(productData)
				}
			}
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
		return ItemViewHolder(
			ProductsListItemBinding.inflate(
				LayoutInflater.from(parent.context),
				parent,
				false
			)
		)
	}

	override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
		val proData = data[position]
		holder.bind(proData)
	}

	override fun getItemCount(): Int = data.size

	override fun getItemViewType(position: Int): Int {
		return VIEW_TYPE_PRODUCT
	}

	companion object {
		const val VIEW_TYPE_PRODUCT = 1
	}

	interface BindImageButtons {
		fun setLikeButton(productId: String, button: CheckBox)
		fun setCartButton(productId: String, imgView: ImageView)
	}

	interface OnClickListener {
		fun onClick(productData: Product)
		fun onDeleteClick(productData: Product)
		fun onEditClick(productId: String) {}
		fun onLikeClick(productId: String) {}
		fun onAddToCartClick(productData: Product) {}
	}
}