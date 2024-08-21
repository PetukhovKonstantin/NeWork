package ru.netology.nework.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentCardUsersBinding
import ru.netology.nework.dto.User

interface OnInteractionListenerUsers {
    fun onTap(user: User) {}
}

class UsersAdapter(
    private val onInteractionListener: OnInteractionListenerUsers = object : OnInteractionListenerUsers {}
) : ListAdapter<User, UserViewHolder>(UserDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding =
            FragmentCardUsersBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        holder.renderingPostStructure(user)
    }
}

class UserDiffCallback : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem == newItem
    }
}

class UserViewHolder(
    private val binding: FragmentCardUsersBinding,
    private val onInteractionListener: OnInteractionListenerUsers
) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun renderingPostStructure(user: User) {
        with(binding) {
            idUser.text = "[${user.id}]"
            loginUser.text = user.login
            nameUser.text = "${user.name}"
            Glide.with(avatar)
                .load(user.avatar)
                .placeholder(R.drawable.ic_image_not_supported_24)
                .error(R.drawable.ic_not_avatars_24)
                .circleCrop()
                .timeout(10_000)
                .into(avatar)
            userListeners(user)
        }
    }

    private fun userListeners(user: User) {
        binding.userCard.setOnClickListener {
            onInteractionListener.onTap(user)
        }
    }
}