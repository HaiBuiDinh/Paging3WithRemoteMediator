package com.example.paging3remotemediator.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.paging3remotemediator.R
import kotlinx.android.synthetic.main.repos_load_state_footer_view_item.view.*

class ReposLoadStateViewHolder(view: View, retry: () -> Unit): RecyclerView.ViewHolder(view) {

    init {
        view.retry_button.setOnClickListener { retry.invoke() }
    }

    companion object {
        fun create(parent: ViewGroup, retry: () -> Unit): ReposLoadStateViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.repos_load_state_footer_view_item, parent, false)
            return ReposLoadStateViewHolder(view, retry)
        }
    }
}