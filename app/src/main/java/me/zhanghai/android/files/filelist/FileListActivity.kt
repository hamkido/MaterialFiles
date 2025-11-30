/*
 * Copyright (c) 2018 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.filelist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java8.nio.file.Path
import java8.nio.file.Paths
import me.zhanghai.android.files.R
import me.zhanghai.android.files.app.AppActivity
import me.zhanghai.android.files.file.MimeType
import me.zhanghai.android.files.settings.Settings
import me.zhanghai.android.files.util.createIntent
import me.zhanghai.android.files.util.extraPath
import me.zhanghai.android.files.util.putArgs
import me.zhanghai.android.files.util.valueCompat

class FileListActivity : AppActivity() {
    private var tabLayout: TabLayout? = null
    private var viewPager: ViewPager2? = null
    private var pagerAdapter: FileListPagerAdapter? = null
    private var tabs: MutableList<Tab> = mutableListOf()
    
    private var singleFragment: FileListFragment? = null
    private var tabLayoutMediator: TabLayoutMediator? = null

    private val isTabsEnabled: Boolean
        get() = Settings.FILE_LIST_TABS_ENABLED.valueCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isTabsEnabled) {
            setContentView(R.layout.file_list_activity)
            setupTabsMode(savedInstanceState)
        } else {
            setupSingleMode(savedInstanceState)
        }
    }

    private fun setupSingleMode(savedInstanceState: Bundle?) {
        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            singleFragment = FileListFragment().putArgs(FileListFragment.Args(intent))
            supportFragmentManager.commit { add(android.R.id.content, singleFragment!!) }
        } else {
            singleFragment = supportFragmentManager.findFragmentById(android.R.id.content)
                as? FileListFragment
        }
    }

    private fun setupTabsMode(savedInstanceState: Bundle?) {
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        if (savedInstanceState != null) {
            @Suppress("DEPRECATION")
            savedInstanceState.getParcelableArrayList<Tab>(STATE_TABS)?.let { savedTabs ->
                tabs.addAll(savedTabs)
            }
        }

        if (tabs.isEmpty()) {
            val initialPath = intent.extraPath ?: Settings.FILE_LIST_DEFAULT_DIRECTORY.valueCompat
            tabs.add(Tab(FileListPagerAdapter.generateTabId(), initialPath))
        }

        pagerAdapter = FileListPagerAdapter(this, tabs)
        viewPager?.adapter = pagerAdapter

        // Show tab bar only when there are multiple tabs
        updateTabLayoutVisibility()

        tabLayoutMediator = TabLayoutMediator(tabLayout!!, viewPager!!) { tab, position ->
            val customView = LayoutInflater.from(this)
                .inflate(R.layout.file_list_tab_item, tabLayout, false)
            val titleView = customView.findViewById<TextView>(R.id.tabTitle)
            val closeButton = customView.findViewById<ImageButton>(R.id.tabCloseButton)

            titleView.text = tabs[position].getDisplayTitle()
            closeButton.setOnClickListener { closeTab(position) }
            closeButton.isVisible = tabs.size > 1

            tab.customView = customView
        }.also { it.attach() }

        viewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateTabTitles()
            }
        })
    }

    private fun updateTabLayoutVisibility() {
        tabLayout?.isVisible = tabs.size > 1
    }

    private fun updateTabTitles() {
        tabLayout?.let { layout ->
            for (i in 0 until layout.tabCount) {
                val tab = layout.getTabAt(i)
                val customView = tab?.customView
                if (customView != null) {
                    val titleView = customView.findViewById<TextView>(R.id.tabTitle)
                    val closeButton = customView.findViewById<ImageButton>(R.id.tabCloseButton)
                    titleView?.text = tabs.getOrNull(i)?.getDisplayTitle() ?: ""
                    closeButton?.isVisible = tabs.size > 1
                }
            }
        }
    }

    fun addNewTab(path: Path? = null) {
        if (!isTabsEnabled) return
        
        val newPath = path ?: Settings.FILE_LIST_DEFAULT_DIRECTORY.valueCompat
        pagerAdapter?.let { adapter ->
            val newPosition = adapter.addTab(newPath)
            updateTabLayoutVisibility()
            viewPager?.setCurrentItem(newPosition, true)
            // TabLayoutMediator automatically syncs with adapter changes via notifyItemInserted
            updateTabTitles()
        }
    }

    fun closeTab(position: Int) {
        if (!isTabsEnabled) return
        
        pagerAdapter?.let { adapter ->
            if (adapter.removeTab(position)) {
                updateTabLayoutVisibility()
                // TabLayoutMediator automatically syncs with adapter changes via notifyItemRemoved
                updateTabTitles()
            }
        }
    }

    fun closeCurrentTab() {
        viewPager?.currentItem?.let { closeTab(it) }
    }

    fun updateCurrentTabPath(path: Path) {
        if (!isTabsEnabled) return
        
        viewPager?.currentItem?.let { position ->
            pagerAdapter?.updateTabPath(position, path)
            updateTabTitles()
        }
    }

    fun getCurrentFragment(): FileListFragment? {
        return if (isTabsEnabled) {
            viewPager?.currentItem?.let { position ->
                val itemId = pagerAdapter?.getItemId(position) ?: return null
                supportFragmentManager.findFragmentByTag("f$itemId") as? FileListFragment
            }
        } else {
            singleFragment
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (isTabsEnabled) {
            outState.putParcelableArrayList(STATE_TABS, ArrayList(tabs))
        }
    }

    override fun onKeyShortcut(keyCode: Int, event: KeyEvent): Boolean {
        val fragment = getCurrentFragment()
        if (fragment?.onKeyShortcut(keyCode, event) == true) {
            return true
        }
        
        // Handle tab shortcuts
        if (isTabsEnabled && event.isCtrlPressed) {
            when (keyCode) {
                KeyEvent.KEYCODE_T -> {
                    addNewTab()
                    return true
                }
                KeyEvent.KEYCODE_W -> {
                    closeCurrentTab()
                    return true
                }
                KeyEvent.KEYCODE_TAB -> {
                    viewPager?.let { pager ->
                        val nextPosition = if (event.isShiftPressed) {
                            (pager.currentItem - 1 + tabs.size) % tabs.size
                        } else {
                            (pager.currentItem + 1) % tabs.size
                        }
                        pager.setCurrentItem(nextPosition, true)
                    }
                    return true
                }
            }
        }
        
        return super.onKeyUp(keyCode, event)
    }

    companion object {
        private const val STATE_TABS = "tabs"

        fun createViewIntent(path: Path): Intent =
            FileListActivity::class.createIntent()
                .setAction(Intent.ACTION_VIEW)
                .apply { extraPath = path }
    }

    class OpenFileContract : ActivityResultContract<List<MimeType>, Path?>() {
        override fun createIntent(context: Context, input: List<MimeType>): Intent =
            FileListActivity::class.createIntent()
                .setAction(Intent.ACTION_OPEN_DOCUMENT)
                .setType(MimeType.ANY.value)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .putExtra(Intent.EXTRA_MIME_TYPES, input.map { it.value }.toTypedArray())

        override fun parseResult(resultCode: Int, intent: Intent?): Path? =
            if (resultCode == RESULT_OK) intent?.extraPath else null
    }

    class CreateFileContract : ActivityResultContract<Triple<MimeType, String?, Path?>, Path?>() {
        override fun createIntent(
            context: Context,
            input: Triple<MimeType, String?, Path?>
        ): Intent =
            FileListActivity::class.createIntent()
                .setAction(Intent.ACTION_CREATE_DOCUMENT)
                .setType(input.first.value)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .apply {
                    input.second?.let { putExtra(Intent.EXTRA_TITLE, it) }
                    input.third?.let { extraPath = it }
                }

        override fun parseResult(resultCode: Int, intent: Intent?): Path? =
            if (resultCode == RESULT_OK) intent?.extraPath else null
    }

    class OpenDirectoryContract : ActivityResultContract<Path?, Path?>() {
        override fun createIntent(context: Context, input: Path?): Intent =
            FileListActivity::class.createIntent()
                .setAction(Intent.ACTION_OPEN_DOCUMENT_TREE)
                .apply { input?.let { extraPath = it } }

        override fun parseResult(resultCode: Int, intent: Intent?): Path? =
            if (resultCode == RESULT_OK) intent?.extraPath else null
    }
}
