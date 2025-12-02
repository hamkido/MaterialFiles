/*
 * Copyright (c) 2024 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.bookmark

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import kotlinx.parcelize.Parcelize
import me.zhanghai.android.files.R
import me.zhanghai.android.files.databinding.FileBookmarkListFragmentBinding
import me.zhanghai.android.files.filelist.FileListActivity
import me.zhanghai.android.files.ui.ScrollingViewOnApplyWindowInsetsListener
import me.zhanghai.android.files.util.ParcelableArgs
import me.zhanghai.android.files.util.fadeToVisibilityUnsafe
import me.zhanghai.android.files.util.getArgsOrNull
import me.zhanghai.android.files.util.finish
import me.zhanghai.android.files.util.startActivitySafe
import java8.nio.file.Paths

class FileBookmarkListFragment : Fragment(), FileBookmarkListAdapter.Listener {
    private lateinit var binding: FileBookmarkListFragmentBinding
    private lateinit var adapter: FileBookmarkListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        FileBookmarkListFragmentBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        activity.setTitle(R.string.file_bookmark_list_title)

        setupMenu()
        setupTabs()
        setupRecyclerView()
        setupFab()

        observeBookmarks()

        // Trigger auto sync when opening bookmark list
        BookmarkSyncEngine.triggerAutoSync()

        // Apply initial filter from args (if any)
        val filterType = arguments?.getArgsOrNull<Args>()?.filterType ?: Args.FILTER_ALL
        val filter = when (filterType) {
            Args.FILTER_FILES -> BookmarkFilter.Files
            Args.FILTER_DIRECTORIES -> BookmarkFilter.Directories
            else -> BookmarkFilter.All
        }
        BookmarkManager.setFilter(filter)
        selectTabForFilter(filter)
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.file_bookmark_list, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                when (menuItem.itemId) {
                    R.id.action_sync_settings -> {
                        showSyncSettings()
                        true
                    }
                    R.id.action_export -> {
                        exportBookmarks()
                        true
                    }
                    R.id.action_import -> {
                        importBookmarks()
                        true
                    }
                    else -> false
                }
        }, viewLifecycleOwner, Lifecycle.State.STARTED)
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.file_bookmark_tab_all))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.file_bookmark_tab_files))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.file_bookmark_tab_directories))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val filter = when (tab.position) {
                    0 -> BookmarkFilter.All
                    1 -> BookmarkFilter.Files
                    2 -> BookmarkFilter.Directories
                    else -> BookmarkFilter.All
                }
                BookmarkManager.setFilter(filter)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun selectTabForFilter(filter: BookmarkFilter) {
        val position = when (filter) {
            BookmarkFilter.All -> 0
            BookmarkFilter.Files -> 1
            BookmarkFilter.Directories -> 2
            is BookmarkFilter.ByTag -> 0
        }
        binding.tabLayout.getTabAt(position)?.select()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(
            requireContext(), RecyclerView.VERTICAL, false
        )
        adapter = FileBookmarkListAdapter(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.setOnApplyWindowInsetsListener(
            ScrollingViewOnApplyWindowInsetsListener(binding.recyclerView)
        )
    }

    private fun setupFab() {
        binding.syncFab.setOnClickListener { syncBookmarks() }
    }

    private fun observeBookmarks() {
        BookmarkManager.filteredBookmarksLiveData.observe(viewLifecycleOwner) { bookmarks ->
            binding.emptyView.fadeToVisibilityUnsafe(bookmarks.isEmpty())
            adapter.replace(bookmarks)
        }
    }

    private fun syncBookmarks() {
        BookmarkSyncEngine.sync { success, message ->
            activity?.runOnUiThread {
                if (success) {
                    Toast.makeText(
                        requireContext(),
                        R.string.file_bookmark_sync_success,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        message ?: getString(R.string.file_bookmark_sync_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showSyncSettings() {
        startActivitySafe(
            BookmarkSyncSettingsActivity::class.java.let {
                android.content.Intent(requireContext(), it)
            }
        )
    }

    private fun exportBookmarks() {
        // TODO: Implement file picker for export
        val json = BookmarkManager.exportToJson()
        Toast.makeText(requireContext(), "Export: ${json.length} chars", Toast.LENGTH_SHORT).show()
    }

    private fun importBookmarks() {
        // TODO: Implement file picker for import
        Toast.makeText(requireContext(), R.string.file_bookmark_import, Toast.LENGTH_SHORT).show()
    }

    override fun onBookmarkClick(bookmark: FileBookmark) {
        val path = Paths.get(bookmark.path)
        val intent = if (bookmark.isDirectory) {
            FileListActivity.createViewIntent(path)
        } else {
            // Navigate to parent directory and highlight the file
            FileListActivity.createViewIntent(path.parent ?: path)
        }
        startActivitySafe(intent)
    }

    override fun onBookmarkEdit(bookmark: FileBookmark) {
        EditFileBookmarkDialogFragment.show(bookmark, this)
    }

    override fun onBookmarkDelete(bookmark: FileBookmark) {
        BookmarkManager.deleteBookmark(bookmark.id)
        Toast.makeText(requireContext(), R.string.file_bookmark_removed, Toast.LENGTH_SHORT).show()
    }

    override fun onBookmarkOpenLocation(bookmark: FileBookmark) {
        val path = Paths.get(bookmark.path)
        val parentPath = path.parent ?: path
        startActivitySafe(FileListActivity.createViewIntent(parentPath))
    }

    fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            return true
        }
        return false
    }

    @Parcelize
    class Args(val filterType: Int = FILTER_ALL) : ParcelableArgs {
        companion object {
            const val FILTER_ALL = 0
            const val FILTER_FILES = 1
            const val FILTER_DIRECTORIES = 2
        }
    }
}

