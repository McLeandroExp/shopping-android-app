package com.vishalgaur.shoppingapp.ui.loginSignup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewbinding.ViewBinding
import com.vishalgaur.shoppingapp.data.UserData
import com.vishalgaur.shoppingapp.ui.MyOnFocusChangeListener
import com.vishalgaur.shoppingapp.ui.home.MainActivity
import com.vishalgaur.shoppingapp.viewModels.AuthViewModel

abstract class LoginSignupBaseFragment<VBinding : ViewBinding> : Fragment() {

	protected val viewModel: AuthViewModel by activityViewModels()

	protected lateinit var binding: VBinding
	protected abstract fun setViewBinding(): VBinding

	protected val focusChangeListener = MyOnFocusChangeListener()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		init()
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		setUpViews()
		observeView()
		return binding.root
	}

	fun launchOtpActivity(from: String, extras: Bundle) {
		val intent = Intent(context, OtpActivity::class.java).putExtra(
			"from",
			from
		).putExtras(extras)
		startActivity(intent)
	}

	fun loginUser(userData: UserData, isRemOn: Boolean) {
		viewModel.login(userData, isRemOn)
		val intent = Intent(context, MainActivity::class.java)
		intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
		startActivity(intent)
		requireActivity().finish()
	}

	fun signUpUser(userData: UserData) {
		viewModel.signUp(userData)
		val intent = Intent(context, MainActivity::class.java)
		intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
		startActivity(intent)
		requireActivity().finish()
	}

	open fun setUpViews() {}

	open fun observeView() {}

	private fun init() {
		binding = setViewBinding()
	}

	interface OnClickListener : View.OnClickListener
}