package com.vishalgaur.shoppingapp.ui.home

import android.annotation.SuppressLint
import android.app.Application
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.PagerSnapHelper
import com.vishalgaur.shoppingapp.R
import com.vishalgaur.shoppingapp.data.utils.AddObjectStatus
import com.vishalgaur.shoppingapp.data.utils.EquipmentTypes
import com.vishalgaur.shoppingapp.data.utils.PersonalCareTypes
import com.vishalgaur.shoppingapp.data.utils.ProductVariants
import com.vishalgaur.shoppingapp.data.utils.StoreDataStatus
import com.vishalgaur.shoppingapp.databinding.FragmentProductDetailsBinding
import com.vishalgaur.shoppingapp.ui.AddItemErrors
import com.vishalgaur.shoppingapp.ui.DotsIndicatorDecoration
import com.vishalgaur.shoppingapp.data.utils.formatToTwoDecimals
import com.vishalgaur.shoppingapp.viewModels.ProductViewModel

class ProductDetailsFragment : Fragment() {

	inner class ProductViewModelFactory(
		private val productId: String,
		private val application: Application
	) : ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel?> create(modelClass: Class<T>): T {
			if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
				return ProductViewModel(productId, application) as T
			}
			throw IllegalArgumentException("Unknown ViewModel Class")
		}
	}

	private lateinit var binding: FragmentProductDetailsBinding
	private lateinit var viewModel: ProductViewModel
	private var selectedSize: String? = null
	private var selectedColor: String? = null

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		binding = FragmentProductDetailsBinding.inflate(layoutInflater)
		val productId = arguments?.getString("productId")

		if (activity != null && productId != null) {
			val viewModelFactory = ProductViewModelFactory(productId, requireActivity().application)
			viewModel = ViewModelProvider(this, viewModelFactory).get(ProductViewModel::class.java)
		}

		if (viewModel.isSeller()) {
			binding.proDetailsAddCartBtn.visibility = View.GONE
		} else {
			binding.proDetailsAddCartBtn.visibility = View.VISIBLE
			binding.proDetailsAddCartBtn.setOnClickListener {
				if (viewModel.isItemInCart.value == true) {
					navigateToCartFragment()
				} else {
					onAddToCart()
					if (viewModel.errorStatus.value?.isEmpty() == true) {
						viewModel.addItemStatus.observe(viewLifecycleOwner) { status ->
							if (status == AddObjectStatus.DONE) {
								makeToast("Producto añadido al carrito")
								viewModel.checkIfInCart()
							}
						}
					}
				}
			}
		}

		binding.loaderLayout.loaderFrameLayout.background =
			ResourcesCompat.getDrawable(resources, R.color.white, null)

		binding.layoutViewsGroup.visibility = View.GONE
		binding.proDetailsAddCartBtn.visibility = View.GONE
		setObservers()
		return binding.root
	}

	override fun onResume() {
		super.onResume()
		viewModel.setLike()
		viewModel.checkIfInCart()
		selectedSize = null
		selectedColor = null
	}

	private fun setObservers() {
		viewModel.dataStatus.observe(viewLifecycleOwner) {
			when (it) {
				StoreDataStatus.DONE -> {
					binding.loaderLayout.loaderFrameLayout.visibility = View.GONE
					binding.proDetailsLayout.visibility = View.VISIBLE
					setViews()
				}
				else -> {
					binding.proDetailsLayout.visibility = View.GONE
					binding.loaderLayout.loaderFrameLayout.visibility = View.VISIBLE
				}
			}
		}
		viewModel.isLiked.observe(viewLifecycleOwner) {
			if (it == true) {
				binding.proDetailsLikeBtn.setImageResource(R.drawable.liked_heart_drawable)
			} else {
				binding.proDetailsLikeBtn.setImageResource(R.drawable.heart_icon_drawable)
			}
		}
		viewModel.isItemInCart.observe(viewLifecycleOwner) {
			if (it == true) {
				binding.proDetailsAddCartBtn.text =
					getString(R.string.pro_details_go_to_cart_btn_text)
			} else {
				binding.proDetailsAddCartBtn.text =
					getString(R.string.pro_details_add_to_cart_btn_text)
			}
		}
		viewModel.errorStatus.observe(viewLifecycleOwner) {
			if (it.isNotEmpty())
				modifyErrors(it)
		}
	}

	@SuppressLint("ResourceAsColor")
	private fun modifyErrors(errList: List<AddItemErrors>) {
		makeToast("Por favor selecciona una variante o tipo.")
		if (!errList.isNullOrEmpty()) {
			errList.forEach { err ->
				when (err) {
					AddItemErrors.ERROR_SIZE -> {
						binding.proDetailsSelectSizeLabel.setTextColor(R.color.red_600)
					}
					AddItemErrors.ERROR_COLOR -> {
						binding.proDetailsSelectColorLabel.setTextColor(R.color.red_600)
					}
				}
			}
		}
	}

	private fun setViews() {
		binding.layoutViewsGroup.visibility = View.VISIBLE
		if (!viewModel.isSeller()) {
			binding.proDetailsAddCartBtn.visibility = View.VISIBLE
		}
		binding.addProAppBar.topAppBar.title = viewModel.productData.value?.name
		binding.addProAppBar.topAppBar.setNavigationOnClickListener {
			findNavController().navigateUp()
		}
		binding.addProAppBar.topAppBar.inflateMenu(R.menu.app_bar_menu)
		binding.addProAppBar.topAppBar.overflowIcon?.setTint(
			ContextCompat.getColor(
				requireContext(),
				R.color.gray
			)
		)

		setImagesView()

		binding.proDetailsTitleTv.text = viewModel.productData.value?.name ?: ""
		binding.proDetailsLikeBtn.apply {
			setOnClickListener {
				viewModel.toggleLikeProduct()
			}
		}
		binding.proDetailsRatingBar.rating = (viewModel.productData.value?.rating ?: 0.0).toFloat()
		binding.proDetailsPriceTv.text = resources.getString(
			R.string.pro_details_price_value,
			viewModel.productData.value?.price?.formatToTwoDecimals() ?: "0.00"
		)
		updateCategorySpecificViews(viewModel.productData.value?.category ?: "")
		binding.proDetailsSpecificationsLabel.text = getString(R.string.pro_details_desc_label_text)
		binding.proDetailsSpecificsText.text = viewModel.productData.value?.description ?: ""
	}

	private fun onAddToCart() {
		viewModel.addToCart(null, selectedColor)
	}

	private fun navigateToCartFragment() {
		findNavController().navigate(R.id.action_productDetailsFragment_to_cartFragment)
	}

	private fun makeToast(text: String) {
		Toast.makeText(context, text, Toast.LENGTH_LONG).show()
	}

	private fun setImagesView() {
		if (context != null) {
			binding.proDetailsImagesRecyclerview.isNestedScrollingEnabled = false
			val adapter = ProductImagesAdapter(
				requireContext(),
				viewModel.productData.value?.images ?: emptyList()
			)
			binding.proDetailsImagesRecyclerview.adapter = adapter
			val rad = resources.getDimension(R.dimen.radius)
			val dotsHeight = resources.getDimensionPixelSize(R.dimen.dots_height)
			val inactiveColor = ContextCompat.getColor(requireContext(), R.color.gray)
			val activeColor = ContextCompat.getColor(requireContext(), R.color.blue_accent_300)
			val itemDecoration =
				DotsIndicatorDecoration(rad, rad * 4, dotsHeight, inactiveColor, activeColor)
			binding.proDetailsImagesRecyclerview.addItemDecoration(itemDecoration)
			PagerSnapHelper().attachToRecyclerView(binding.proDetailsImagesRecyclerview)
		}
	}

	private fun updateCategorySpecificViews(category: String) {
		// Default: Hide all specific views and containers
		binding.proDetailsSelectSizeLabel.visibility = View.GONE
		binding.proSizeRadioScroll.visibility = View.GONE
		binding.proDetailsSelectColorLabel.visibility = View.GONE
		binding.proColorRadioScroll.visibility = View.GONE
		binding.proDetailsVariantsInfoTv.visibility = View.GONE

		when (category) {
			"Medicamentos", "Suplementos" -> {
				binding.proDetailsSelectColorLabel.visibility = View.VISIBLE
				binding.proColorRadioScroll.visibility = View.VISIBLE
				binding.proDetailsSelectColorLabel.text = getString(R.string.pro_details_type_doses_label)
				setCategorySpecificButtons(ProductVariants)
			}
			"Equipos Médicos" -> {
				binding.proDetailsSelectColorLabel.visibility = View.VISIBLE
				binding.proDetailsVariantsInfoTv.visibility = View.VISIBLE
				binding.proDetailsSelectColorLabel.text = getString(R.string.pro_details_type_doses_label)
				
				// Show info as text instead of selection
				val availableTypes = viewModel.productData.value?.availableTypes ?: emptyList()
				if (availableTypes.isNotEmpty()) {
					binding.proDetailsVariantsInfoTv.text = availableTypes.joinToString(", ")
					selectedColor = availableTypes[0] // Auto-select the first one
				} else {
					binding.proDetailsVariantsInfoTv.text = "No especificado"
				}
			}
			"Cuidado Personal" -> {
				binding.proDetailsSelectColorLabel.visibility = View.VISIBLE
				binding.proColorRadioScroll.visibility = View.VISIBLE
				binding.proDetailsSelectColorLabel.text = getString(R.string.pro_details_type_doses_label)
				setCategorySpecificButtons(PersonalCareTypes)
			}
			else -> {
				// Show original size labels for other categories if any
				binding.proDetailsSelectSizeLabel.visibility = View.VISIBLE
				binding.proSizeRadioScroll.visibility = View.VISIBLE
			}
		}
	}


	private fun setCategorySpecificButtons(optionsMap: Map<String, String>) {
		binding.proDetailsColorsRadioGroup.apply {
			removeAllViews()
			for ((k, v) in optionsMap) {
				if (viewModel.productData.value?.availableTypes?.contains(k) == true) {
					val radioButton = RadioButton(context)
					radioButton.id = View.generateViewId()
					radioButton.tag = k
					setupRadioButton(radioButton, k)
					radioButton.setOnCheckedChangeListener { buttonView, isChecked ->
						if (isChecked) selectedColor = buttonView.tag.toString()
					}
					addView(radioButton)
				}
			}
			invalidate()
		}
	}

	private fun setupRadioButton(radioButton: RadioButton, text: String, colorHex: String? = null) {
		val param = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
		param.setMargins(resources.getDimensionPixelSize(R.dimen.radio_margin_size))
		radioButton.layoutParams = param
		
		if (colorHex != null) {
			radioButton.background = ContextCompat.getDrawable(requireContext(), R.drawable.color_radio_selector)
			radioButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(colorHex))
			radioButton.setButtonDrawable(R.color.transparent)
		} else {
			radioButton.background = ContextCompat.getDrawable(requireContext(), R.drawable.variant_selector)
			radioButton.setButtonDrawable(null)
			radioButton.text = text
			radioButton.setTextColor(Color.BLACK)
			radioButton.setTypeface(null, Typeface.BOLD)
			radioButton.textAlignment = View.TEXT_ALIGNMENT_CENTER
		}
	}
}