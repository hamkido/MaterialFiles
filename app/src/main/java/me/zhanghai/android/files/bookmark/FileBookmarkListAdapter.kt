/*
 * Copyright (c) 2024 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.bookmark

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import me.zhanghai.android.files.R
import me.zhanghai.android.files.databinding.FileBookmarkItemBinding
import me.zhanghai.android.files.util.layoutInflater

class FileBookmarkListAdapter(
    private val listener: Listener
) : RecyclerView.Adapter<FileBookmarkListAdapter.ViewHolder>() {

    private var bookmarks: List<FileBookmark> = emptyList()

    fun replace(newBookmarks: List<FileBookmark>) {
        bookmarks = newBookmarks
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = bookmarks.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            FileBookmarkItemBinding.inflate(parent.context.layoutInflater, parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bookmark = bookmarks[position]
        holder.bind(bookmark)
    }

    inner class ViewHolder(
        private val binding: FileBookmarkItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val bookmark = bookmarks.getOrNull(bindingAdapterPosition) ?: return@setOnClickListener
                listener.onBookmarkClick(bookmark)
            }
            binding.menuButton.setOnClickListener { view ->
                val bookmark = bookmarks.getOrNull(bindingAdapterPosition) ?: return@setOnClickListener
                showPopupMenu(view, bookmark)
            }
        }

        fun bind(bookmark: FileBookmark) {
            binding.iconImage.setImageResource(
                if (bookmark.isDirectory) {
                    R.drawable.directory_icon_white_24dp
                } else {
                    R.drawable.file_icon_white_24dp
                }
            )
            binding.nameText.text = bookmark.name
            binding.pathText.text = bookmark.path

            // Setup tags
            binding.tagsChipGroup.removeAllViews()
            if (bookmark.tags.isNotEmpty()) {
                binding.tagsChipGroup.isVisible = true
                bookmark.tags.forEach { tag ->
                    val chip = Chip(binding.root.context).apply {
                        text = tag
                        isClickable = false
                        isCheckable = false
                        textSize = 10f
                        chipMinHeight = 24f.dpToPx(context)
                    }
                    binding.tagsChipGroup.addView(chip)
                }
            } else {
                binding.tagsChipGroup.isVisible = false
            }
        }

        private fun showPopupMenu(view: View, bookmark: FileBookmark) {
            PopupMenu(view.context, view).apply {
                menuInflater.inflate(R.menu.file_bookmark_item, menu)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_edit -> {
                            listener.onBookmarkEdit(bookmark)
                            true
                        }
                        R.id.action_open_location -> {
                            listener.onBookmarkOpenLocation(bookmark)
                            true
                        }
                        R.id.action_delete -> {
                            listener.onBookmarkDelete(bookmark)
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
        }

        private fun Float.dpToPx(context: android.content.Context): Float =
            this * context.resources.displayMetrics.density
    }

    interface Listener {
        fun onBookmarkClick(bookmark: FileBookmark)
        fun onBookmarkEdit(bookmark: FileBookmark)
        fun onBookmarkDelete(bookmark: FileBookmark)
        fun onBookmarkOpenLocation(bookmark: FileBookmark)
    }
}

