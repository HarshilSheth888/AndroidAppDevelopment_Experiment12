package com.example.webservicedemo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.webservicedemo.data.model.PostItem
import com.example.webservicedemo.databinding.ItemPostCardBinding

class PostAdapter(
    private val onInspectJson: (PostItem) -> Unit,
    private val onEdit: (PostItem) -> Unit,
    private val onDelete: (PostItem) -> Unit
) : ListAdapter<PostItem, PostAdapter.PostViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(private val binding: ItemPostCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PostItem) {
            val context = binding.root.context
            binding.tvPostIdBadge.text = context.getString(com.example.webservicedemo.R.string.post_id_format, item.id)
            binding.tvUserBadge.text = context.getString(com.example.webservicedemo.R.string.user_format, item.userId)
            binding.tvPostTitle.text = item.title.replaceFirstChar { it.uppercase() }
            binding.tvPostBody.text = item.body

            binding.btnInspectJson.setOnClickListener {
                onInspectJson(item)
            }

            binding.btnEditPost.setOnClickListener {
                onEdit(item)
            }

            binding.btnDeletePost.setOnClickListener {
                onDelete(item)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PostItem>() {
            override fun areItemsTheSame(oldItem: PostItem, newItem: PostItem): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: PostItem, newItem: PostItem): Boolean =
                oldItem == newItem
        }
    }
}
