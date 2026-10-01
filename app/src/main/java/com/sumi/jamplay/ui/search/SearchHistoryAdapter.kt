package com.sumi.jamplay.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sumi.jamplay.databinding.ItemSearchHistoryBinding

class SearchHistoryAdapter(private val onClick: (String) -> Unit) : ListAdapter<String, SearchHistoryAdapter.Holder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemSearchHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(private val binding: ItemSearchHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(keyword: String) {
            binding.root.text = keyword
            binding.root.setOnClickListener { onClick(keyword) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(old: String, new: String) = old == new
        override fun areContentsTheSame(old: String, new: String) = old == new
    }
}
