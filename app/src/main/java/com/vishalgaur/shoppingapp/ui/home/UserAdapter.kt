package com.vishalgaur.shoppingapp.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vishalgaur.shoppingapp.R
import com.vishalgaur.shoppingapp.data.UserData
import com.vishalgaur.shoppingapp.databinding.UsersListItemBinding

class UserAdapter(
    private var users: List<UserData>,
    private val onClickListener: OnClickListener
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: UsersListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(user: UserData) {
            binding.userNameTv.text = user.name
            binding.userEmailTv.text = user.email
            binding.userTypeTv.text = user.userType
            
            // Set background color based on user type
            val context = binding.root.context
            val backgroundRes = if (user.userType == "SELLER") {
                R.color.blue_500
            } else if (user.userType == "ADMIN") {
                R.color.red_600
            } else {
                R.color.green_500
            }
            binding.userTypeTv.setBackgroundColor(context.getColor(backgroundRes))
            
            binding.userDeleteButton.setOnClickListener {
                onClickListener.onDeleteClick(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = UsersListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    fun dataChanged(newUsers: List<UserData>) {
        users = newUsers
        notifyDataSetChanged()
    }

    interface OnClickListener {
        fun onDeleteClick(user: UserData)
    }
}
