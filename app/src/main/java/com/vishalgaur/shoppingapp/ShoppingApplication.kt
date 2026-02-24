package com.vishalgaur.shoppingapp

import android.app.Application
import com.vishalgaur.shoppingapp.data.source.repository.AuthRepoInterface
import com.vishalgaur.shoppingapp.data.source.repository.ProductsRepoInterface

class ShoppingApplication : Application() {
	val authRepository: AuthRepoInterface
		get() = ServiceLocator.provideAuthRepository(this)

	val productsRepository: ProductsRepoInterface
		get() = ServiceLocator.provideProductsRepository(this)

	val settingsRepository: com.vishalgaur.shoppingapp.data.source.repository.SettingsRepository
		get() = ServiceLocator.provideSettingsRepository()

	override fun onCreate() {
		super.onCreate()
	}
}