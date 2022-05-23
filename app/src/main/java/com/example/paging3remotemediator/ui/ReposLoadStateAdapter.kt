package com.example.paging3remotemediator.ui

import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import kotlinx.android.synthetic.main.repos_load_state_footer_view_item.view.*

class ReposLoadStateAdapter(private val retry: () -> Unit) : LoadStateAdapter<ReposLoadStateViewHolder>() {
    override fun onBindViewHolder(holder: ReposLoadStateViewHolder, loadState: LoadState) {
        if (loadState is LoadState.Error) {
            holder.itemView.error_msg.text = loadState.error.localizedMessage
        }
        holder.itemView.progress_bar.isVisible = loadState is LoadState.Loading
        holder.itemView.retry_button.isVisible = loadState is LoadState.Error
        holder.itemView.error_msg.isVisible = loadState is LoadState.Error
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ): ReposLoadStateViewHolder {
        return ReposLoadStateViewHolder.create(parent, retry)
    }
}