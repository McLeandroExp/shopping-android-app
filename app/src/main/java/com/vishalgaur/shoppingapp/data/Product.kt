package com.vishalgaur.shoppingapp.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "products")
data class Product @JvmOverloads constructor(
	@PrimaryKey
	var productId: String = "",
	var name: String = "",
	var owner: String = "",
	var description: String = "",
	var category: String = "",
	var price: Double = 0.0,
	var mrp: Double = 0.0,
	@get:PropertyName("tipo")
	@set:PropertyName("tipo")
	var availableTypes: List<String> = ArrayList(),
	var images: List<String> = ArrayList(),
	var rating: Double = 0.0
) : Parcelable {
	fun toHashMap(): HashMap<String, Any> {
		return hashMapOf(
			"productId" to productId,
			"name" to name,
			"owner" to owner,
			"description" to description,
			"category" to category,
			"price" to price,
			"mrp" to mrp,
			"tipo" to availableTypes,
			"images" to images,
			"rating" to rating
		)
	}
}