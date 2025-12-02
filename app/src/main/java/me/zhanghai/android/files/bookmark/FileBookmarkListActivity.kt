/*
 * Copyright (c) 2024 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.bookmark

import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.commit
import me.zhanghai.android.files.app.AppActivity

class FileBookmarkListActivity : AppActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<android.view.View>(android.R.id.content)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                add(android.R.id.content, FileBookmarkListFragment::class.java, null)
            }
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val fragment = supportFragmentManager.findFragmentById(android.R.id.content)
        return (fragment as? FileBookmarkListFragment)?.onKeyUp(keyCode, event)
            ?: super.onKeyUp(keyCode, event)
    }
}

