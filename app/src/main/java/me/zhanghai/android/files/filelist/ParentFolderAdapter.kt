/*
 * Copyright (c) 2024 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.filelist

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java8.nio.file.Path
import me.zhanghai.android.files.R
import me.zhanghai.android.files.compat.getDrawableCompat
import me.zhanghai.android.files.databinding.ParentFolderItemBinding
import me.zhanghai.android.files.file.FileItem
import me.zhanghai.android.files.file.iconRes
import me.zhanghai.android.files.ui.AnimatedListAdapter
import me.zhanghai.android.files.ui.CheckableItemBackground
import me.zhanghai.android.files.util.isMaterial3Theme
import me.zhanghai.android.files.util.layoutInflater

class ParentFolderAdapter(
    private val listener: Listener
) : AnimatedListAdapter<FileItem, ParentFolderAdapter.ViewHolder>(CALLBACK) {

    private var currentPath: Path? = null

    fun setCurrentPath(path: Path?) {
        currentPath = path
        notifyItemRangeChanged(0, itemCount, PAYLOAD_STATE_CHANGED)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ParentFolderItemBinding.inflate(parent.context.layoutInflater, parent, false)
        ).apply {
            binding.itemLayout.background = if (binding.itemLayout.context.isMaterial3Theme) {
                CheckableItemBackground.create(0f, 0f, binding.itemLayout.context)
            } else {
                null
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        throw UnsupportedOperationException()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        val file = getItem(position)
        val binding = holder.binding
        val path = file.path
        binding.itemLayout.isChecked = path == currentPath
        if (payloads.isNotEmpty()) {
            return
        }
        binding.itemLayout.setOnClickListener { listener.onItemClicked(file) }
        binding.iconImage.setImageDrawable(
            binding.iconImage.context.getDrawableCompat(file.mimeType.iconRes)
        )
        binding.nameText.text = file.name
    }

    interface Listener {
        fun onItemClicked(file: FileItem)
    }

    class ViewHolder(val binding: ParentFolderItemBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val PAYLOAD_STATE_CHANGED = Any()

        private val CALLBACK = object : DiffUtil.ItemCallback<FileItem>() {
            override fun areItemsTheSame(oldItem: FileItem, newItem: FileItem): Boolean =
                oldItem.path == newItem.path

            override fun areContentsTheSame(oldItem: FileItem, newItem: FileItem): Boolean =
                oldItem == newItem
        }
    }
}
