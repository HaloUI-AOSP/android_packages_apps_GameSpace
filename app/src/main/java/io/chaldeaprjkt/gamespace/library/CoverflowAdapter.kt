/*
 * SPDX-FileCopyrightText: 2026 haloUI-AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package io.chaldeaprjkt.gamespace.library

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.chaldeaprjkt.gamespace.R

data class LibraryEntry(
    val packageName: String,
    val mode: Int,
    val label: String,
    val icon: Drawable,
    val banner: Drawable?
)

class CoverflowAdapter : RecyclerView.Adapter<CoverflowAdapter.ViewHolder>() {

    var onItemClick: ((Int) -> Unit)? = null

    private val items = mutableListOf<LibraryEntry>()

    fun get(position: Int) = items.getOrNull(position)

    fun submit(entries: List<LibraryEntry>) {
        items.clear()
        items.addAll(entries)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.coverflow_item, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position]) { clicked -> onItemClick?.invoke(clicked) }
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(android.R.id.icon)
        private val banner: ImageView = view.findViewById(R.id.banner)

        fun bind(entry: LibraryEntry, onClick: (Int) -> Unit) {
            val image = if (entry.banner != null) banner else icon
            banner.isVisible = image === banner
            icon.isVisible = image === icon
            image.setImageDrawable(entry.banner ?: entry.icon)
            image.contentDescription = entry.label
            itemView.setOnClickListener {
                onClick(bindingAdapterPosition)
            }
        }
    }
}
