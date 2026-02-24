package com.vishalgaur.shoppingapp.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.vishalgaur.shoppingapp.ShoppingApplication
import com.vishalgaur.shoppingapp.data.Product
import com.vishalgaur.shoppingapp.data.Result
import com.vishalgaur.shoppingapp.data.Result.Error
import com.vishalgaur.shoppingapp.data.Result.Success
import com.vishalgaur.shoppingapp.data.ShoppingAppSessionManager
import com.vishalgaur.shoppingapp.data.UserData
import com.vishalgaur.shoppingapp.data.utils.StoreDataStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.Month
import java.util.*

private const val TAG = "HomeViewModel"

class HomeViewModel(application: Application) : AndroidViewModel(application) {

	private val productsRepository =
		(application.applicationContext as ShoppingApplication).productsRepository
	private val authRepository =
		(application.applicationContext as ShoppingApplication).authRepository

	private val sessionManager = ShoppingAppSessionManager(application.applicationContext)
	private val currentUser = sessionManager.getUserIdFromSession()
	val isUserASeller = sessionManager.isUserSeller()

	private val _shouldShowOnlyMine = MutableLiveData<Boolean>(false)

	val allProducts: LiveData<List<Product>> = Transformations.switchMap(_shouldShowOnlyMine) { onlyMine ->
		if (onlyMine) {
			Transformations.switchMap(productsRepository.observeProductsByOwner(currentUser!!)) {
				getProductsLiveData(it)
			}
		} else {
			Transformations.switchMap(productsRepository.observeProducts()) {
				getProductsLiveData(it)
			}
		}
	}

	private var _userProducts = MutableLiveData<List<Product>>()
	val userProducts: LiveData<List<Product>> get() = _userProducts

	private var _userOrders = MutableLiveData<List<UserData.OrderItem>>()
	val userOrders: LiveData<List<UserData.OrderItem>> get() = _userOrders

	private var _userAddresses = MutableLiveData<List<UserData.Address>>()
	val userAddresses: LiveData<List<UserData.Address>> get() = _userAddresses

	private var _selectedOrder = MutableLiveData<UserData.OrderItem?>()
	val selectedOrder: LiveData<UserData.OrderItem?> get() = _selectedOrder

	private var _orderProducts = MutableLiveData<List<Product>>()
	val orderProducts: LiveData<List<Product>> get() = _orderProducts

	private var _likedProducts = MutableLiveData<List<Product>>()
	val likedProducts: LiveData<List<Product>> get() = _likedProducts

	private var _userLikes = MutableLiveData<List<String>>()
	val userLikes: LiveData<List<String>> get() = _userLikes

	private var _allUsers = MutableLiveData<List<UserData>>()
	val allUsers: LiveData<List<UserData>> get() = _allUsers

	private var _filterCategory = MutableLiveData("Todos")
	val filterCategory: LiveData<String> get() = _filterCategory

	private val _storeDataStatus = MutableLiveData<StoreDataStatus>()
	val storeDataStatus: LiveData<StoreDataStatus> get() = _storeDataStatus

	private val _dataStatus = MutableLiveData<StoreDataStatus>()
	val dataStatus: LiveData<StoreDataStatus> get() = _dataStatus

	private val _userData = MutableLiveData<UserData?>()
	val userData: LiveData<UserData?> get() = _userData

	private val _products = MediatorLiveData<List<Product>>()
	val products: LiveData<List<Product>> get() = _products

	init {
		viewModelScope.launch {
			authRepository.hardRefreshUserData()
			getUserLikes()
			getUserData()
		}

		_products.addSource(allProducts) {
			filterProducts(_filterCategory.value ?: "Todos")
		}
	}

	fun setDataLoaded() {
		_storeDataStatus.value = StoreDataStatus.DONE
	}

	fun isProductLiked(productId: String): Boolean {
		return _userLikes.value?.contains(productId) == true
	}

	fun toggleLikeByProductId(productId: String) {
		Log.d(TAG, "Toggling Like")
		viewModelScope.launch {
			val isLiked = isProductLiked(productId)
			val allLikes = _userLikes.value?.toMutableList() ?: mutableListOf()
			val deferredRes = async {
				if (isLiked) {
					authRepository.removeProductFromLikes(productId, currentUser!!)
				} else {
					authRepository.insertProductToLikes(productId, currentUser!!)
				}
			}
			val res = deferredRes.await()
			if (res is Success) {
				if (isLiked) {
					allLikes.remove(productId)
				} else {
					allLikes.add(productId)
				}
				_userLikes.value = allLikes
				val proList = _likedProducts.value?.toMutableList() ?: mutableListOf()
				val pro = proList.find { it.productId == productId }
				if (pro != null) {
					proList.remove(pro)
				}
				_likedProducts.value = proList
				Log.d(TAG, "onToggleLike: Success")
			} else {
				if (res is Error) {
					Log.d(TAG, "onToggleLike: Error, ${res.exception}")
				}
			}
		}
	}

	fun isProductInCart(productId: String): Boolean {
		return _userData.value?.cart?.any { it.productId == productId } == true
	}

	fun toggleProductInCart(product: Product) {
		val cartItem = _userData.value?.cart?.find { it.productId == product.productId }
		if (cartItem != null) {
			// Remove from cart
			viewModelScope.launch {
				val res = authRepository.deleteCartItemByUserId(cartItem.itemId, currentUser!!)
				if (res is Success) {
					getUserData()
				}
			}
		} else {
			// Add to cart
			val isSizesRequired = product.category in listOf("Medicamentos", "Suplementos")
			val isColorsRequired = product.category == "Equipos Médicos"
			
			if (isSizesRequired || isColorsRequired) {
				// Cannot add directly without variant, maybe we should trigger a toast or something
				// But since we can't easily toast from here without a context/event, 
				// let's assume we want to guide user to details if they click add.
				// However, if it's medical, it's safer to not add it.
				return
			}
			
			viewModelScope.launch {
				val itemId = UUID.randomUUID().toString()
				val newItem = UserData.CartItem(
					itemId, product.productId, product.owner, 1, null
				)
				val res = authRepository.insertCartItemByUserId(newItem, currentUser!!)
				if (res is Success) {
					getUserData()
				}
			}
		}
	}

	fun setDataLoading() {
		_dataStatus.value = StoreDataStatus.LOADING
	}

	fun setSourceToAll() {
		_shouldShowOnlyMine.value = false
		refreshProducts()
	}

	fun setSourceToMine() {
		_shouldShowOnlyMine.value = true
		refreshProducts()
	}

	fun getUserLikes() {
		viewModelScope.launch {
			val res = authRepository.getLikesByUserId(currentUser!!)
			if (res is Success) {
				val data = res.data ?: emptyList()
				if (data[0] != "") {
					_userLikes.value = data
				} else {
					_userLikes.value = emptyList()
				}
				Log.d(TAG, "Getting Likes: Success")
			} else {
				_userLikes.value = emptyList()
				if (res is Error)
					Log.d(TAG, "Getting Likes: Error, ${res.exception}")
			}
		}
	}

	fun getLikedProducts() {
		val res: List<Product> = if (_userLikes.value != null) {
			val allLikes = _userLikes.value ?: emptyList()
			if (!allLikes.isNullOrEmpty()) {
				Log.d(TAG, "alllikes = ${allLikes.size}")
				_dataStatus.value = StoreDataStatus.DONE
				allLikes.map { proId ->
					allProducts.value?.find { it.productId == proId } ?: Product()
				}
			} else {
				_dataStatus.value = StoreDataStatus.ERROR
				emptyList()
			}
		} else {
			_dataStatus.value = StoreDataStatus.ERROR
			emptyList()
		}
		_likedProducts.value = res
	}

	private fun getProductsLiveData(result: Result<List<Product>?>?): LiveData<List<Product>> {
		val res = MutableLiveData<List<Product>>()
		if (result is Success) {
			Log.d(TAG, "result is success")
			_storeDataStatus.value = StoreDataStatus.DONE
			res.value = result.data ?: emptyList()
		} else {
			Log.d(TAG, "result is not success")
			res.value = emptyList()
			_storeDataStatus.value = StoreDataStatus.ERROR
			if (result is Error)
				Log.d(TAG, "getProductsLiveData: Error Occurred: ${result.exception}")
		}
		return res
	}


	fun refreshProducts() {
		viewModelScope.launch {
			_storeDataStatus.value = StoreDataStatus.LOADING
			val res = async { productsRepository.refreshProducts() }
			res.await()
			Log.d(TAG, "refreshProducts: status = ${_storeDataStatus.value}")
		}
	}

	fun filterBySearch(queryText: String) {
		filterProducts(_filterCategory.value!!)
		_products.value = _products.value?.filter { product ->
			product.name.contains(queryText, true) or
					((queryText.toDoubleOrNull() ?: 0.0).compareTo(product.price) == 0)
		}
	}

	fun filterProducts(filterType: String) {
		Log.d(TAG, "filterType is $filterType")
		_filterCategory.value = filterType
		val allPros = allProducts.value ?: emptyList()
		_products.value = when (filterType) {
			"Ninguno" -> emptyList()
			"Todos" -> allPros
			else -> allPros.filter { product ->
				product.category == filterType
			}
		}
	}

	fun deleteProduct(productId: String) {
		viewModelScope.launch {
			val delRes = async { productsRepository.deleteProductById(productId) }
			when (val res = delRes.await()) {
				is Success -> Log.d(TAG, "onDelete: Success")
				is Error -> Log.d(TAG, "onDelete: Error, ${res.exception}")
				else -> Log.d(TAG, "onDelete: Some error occurred!")
			}
		}
	}

	fun signOut() {
		viewModelScope.launch {
			val deferredRes = async { authRepository.signOut() }
			deferredRes.await()
		}
	}

	fun getAllOrders() {
		viewModelScope.launch {
			_storeDataStatus.value = StoreDataStatus.LOADING
			val deferredRes = async { authRepository.getOrdersByUserId(currentUser!!) }
			val res = deferredRes.await()
			if (res is Success) {
				_userOrders.value = res.data ?: emptyList()
				_storeDataStatus.value = StoreDataStatus.DONE
				Log.d(TAG, "Getting Orders: Success")
			} else {
				_userOrders.value = emptyList()
				_storeDataStatus.value = StoreDataStatus.ERROR
				if (res is Error)
					Log.d(TAG, "Getting Orders: Error, ${res.exception}")
			}
		}
	}

	fun getOrderDetailsByOrderId(orderId: String) {
		viewModelScope.launch {
			_storeDataStatus.value = StoreDataStatus.LOADING
			if (_userOrders.value != null) {
				val orderData = _userOrders.value!!.find { it.orderId == orderId }
				if (orderData != null) {
					_selectedOrder.value = orderData
					_orderProducts.value =
						orderData.items.map {
							allProducts.value?.find { pro -> pro.productId == it.productId }
								?: Product()
						}
					_storeDataStatus.value = StoreDataStatus.DONE
				} else {
					_selectedOrder.value = null
					_storeDataStatus.value = StoreDataStatus.ERROR
				}
			}
		}
	}

	fun onSetStatusOfOrder(orderId: String, status: String) {
		val currDate = Calendar.getInstance()
		val dateString =
			"${Month.values()[(currDate.get(Calendar.MONTH))].name} ${
				currDate.get(Calendar.DAY_OF_MONTH)
			}, ${currDate.get(Calendar.YEAR)}"
		Log.d(TAG, "Selected Status is $status ON $dateString")
		setStatusOfOrder(orderId, "$status ON $dateString")
	}

	private fun setStatusOfOrder(orderId: String, statusString: String) {
		viewModelScope.launch {
			_storeDataStatus.value = StoreDataStatus.LOADING
			val deferredRes = async {
				authRepository.setStatusOfOrder(orderId, currentUser!!, statusString)
			}
			val res = deferredRes.await()
			if (res is Success) {
				val orderData = _selectedOrder.value
				orderData?.status = statusString
				_selectedOrder.value = orderData
				getOrderDetailsByOrderId(orderId)
			} else {
				_storeDataStatus.value = StoreDataStatus.ERROR
				if (res is Error)
					Log.d(TAG, "Error updating status, ${res.exception}")
			}
		}
	}

	fun getUserAddresses() {
		Log.d(TAG, "Getting Addresses")
		_dataStatus.value = StoreDataStatus.LOADING
		viewModelScope.launch {
			val res = authRepository.getAddressesByUserId(currentUser!!)
			if (res is Success) {
				_userAddresses.value = res.data ?: emptyList()
				_dataStatus.value = StoreDataStatus.DONE
				Log.d(TAG, "Getting Addresses: Success")
			} else {
				_userAddresses.value = emptyList()
				_dataStatus.value = StoreDataStatus.ERROR
				if (res is Error)
					Log.d(TAG, "Getting Addresses: Error Occurred, ${res.exception.message}")
			}
		}
	}

	fun deleteAddress(addressId: String) {
		viewModelScope.launch {
			val delRes = async { authRepository.deleteAddressById(addressId, currentUser!!) }
			when (val res = delRes.await()) {
				is Success -> {
					Log.d(TAG, "onDeleteAddress: Success")
					val addresses = _userAddresses.value?.toMutableList()
					addresses?.let {
						val pos =
							addresses.indexOfFirst { address -> address.addressId == addressId }
						if (pos >= 0)
							it.removeAt(pos)
						_userAddresses.value = it
					}
				}
				is Error -> Log.d(TAG, "onDeleteAddress: Error, ${res.exception}")
				else -> Log.d(TAG, "onDeleteAddress: Some error occurred!")
			}
		}
	}

	fun getUserData() {
		viewModelScope.launch {
			_dataStatus.value = StoreDataStatus.LOADING
			val deferredRes = async { authRepository.getUserData(currentUser!!) }
			val res = deferredRes.await()
			if (res is Success) {
				val uData = res.data
				_userData.value = uData
				_dataStatus.value = StoreDataStatus.DONE
			} else {
				_dataStatus.value = StoreDataStatus.ERROR
				_userData.value = null
			}
		}
	}

    fun fetchAllUsers() {
        viewModelScope.launch {
            _dataStatus.value = StoreDataStatus.LOADING
            val res = authRepository.getAllUsers()
            if (res is Success) {
                _allUsers.value = res.data
                _dataStatus.value = StoreDataStatus.DONE
            } else {
                _allUsers.value = emptyList()
                _dataStatus.value = StoreDataStatus.ERROR
            }
        }
    }

    fun deleteUserAccount(userId: String) {
        viewModelScope.launch {
            _dataStatus.value = StoreDataStatus.LOADING
            val res = authRepository.deleteUser(userId)
            if (res is Success) {
                fetchAllUsers()
            } else {
                _dataStatus.value = StoreDataStatus.ERROR
            }
        }
    }
}