package com.vishalgaur.shoppingapp.ui.home

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.vishalgaur.shoppingapp.R
import com.vishalgaur.shoppingapp.data.ShoppingAppSessionManager
import com.vishalgaur.shoppingapp.databinding.ActivityMainBinding

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

	private lateinit var binding: ActivityMainBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		Log.d(TAG, "onCreate starts")
		super.onCreate(savedInstanceState)
		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)

		// Bottom Navigation
		setUpNav()
	}

	private fun setUpNav() {
		val navFragment =
			supportFragmentManager.findFragmentById(R.id.home_nav_host_fragment) as NavHostFragment
		val navController = navFragment.navController
		val sessionManager = ShoppingAppSessionManager(this.applicationContext)

		if (sessionManager.isUserSeller()) {
			val navGraph = navController.navInflater.inflate(R.navigation.home_nav_graph)
			navGraph.setStartDestination(R.id.myProductsFragment)
			navController.graph = navGraph
		}

		NavigationUI.setupWithNavController(binding.homeBottomNavigation, navController)

		navFragment.navController.addOnDestinationChangedListener { _, destination, _ ->
			when (destination.id) {
				R.id.homeFragment -> setBottomNavVisibility(View.VISIBLE)
				R.id.cartFragment -> setBottomNavVisibility(View.VISIBLE)
				R.id.accountFragment -> setBottomNavVisibility(View.VISIBLE)
				R.id.ordersFragment -> setBottomNavVisibility(View.VISIBLE)
				R.id.myProductsFragment -> setBottomNavVisibility(View.VISIBLE)
				R.id.orderSuccessFragment -> setBottomNavVisibility(View.VISIBLE)
				else -> setBottomNavVisibility(View.GONE)
			}
		}

		if (sessionManager.isUserSeller()) {
			binding.homeBottomNavigation.menu.removeItem(R.id.homeFragment)
			binding.homeBottomNavigation.menu.removeItem(R.id.cartFragment)
		} else {
			binding.homeBottomNavigation.menu.removeItem(R.id.myProductsFragment)
		}
	}

	private fun setBottomNavVisibility(visibility: Int) {
		binding.homeBottomNavigation.visibility = visibility
	}
}