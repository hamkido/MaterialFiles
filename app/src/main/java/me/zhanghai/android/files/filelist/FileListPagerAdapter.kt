/*
 * Copyright (c) 2024 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.filelist

import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.util.concurrent.atomic.AtomicLong
import java8.nio.file.Path
import me.zhanghai.android.files.util.extraPath
import me.zhanghai.android.files.util.putArgs

class FileListPagerAdapter(
    activity: FragmentActivity,
    private val tabs: MutableList<Tab>
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): Fragment {
        val tab = tabs[position]
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            extraPath = tab.path
        }
        return FileListFragment().putArgs(FileListFragment.Args(intent))
    }

    override fun getItemId(position: Int): Long = tabs[position].id

    override fun containsItem(itemId: Long): Boolean = tabs.any { it.id == itemId }

    fun addTab(path: Path): Int {
        val id = generateTabId()
        val tab = Tab(id, path)
        tabs.add(tab)
        notifyItemInserted(tabs.size - 1)
        return tabs.size - 1
    }

    fun removeTab(position: Int): Boolean {
        if (tabs.size <= 1) return false
        tabs.removeAt(position)
        notifyItemRemoved(position)
        return true
    }

    fun getTab(position: Int): Tab = tabs[position]

    fun updateTabPath(position: Int, path: Path) {
        if (position in tabs.indices) {
            tabs[position] = tabs[position].copy(path = path)
        }
    }

    companion object {
        private val nextId = AtomicLong(System.currentTimeMillis())

        fun generateTabId(): Long = nextId.incrementAndGet()
    }
}

