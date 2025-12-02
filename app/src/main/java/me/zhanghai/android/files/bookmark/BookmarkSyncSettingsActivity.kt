/*
 * Copyright (c) 2024 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.bookmark

import android.os.Bundle
import androidx.fragment.app.commit
import me.zhanghai.android.files.app.AppActivity

class BookmarkSyncSettingsActivity : AppActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<android.view.View>(android.R.id.content)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                add(android.R.id.content, BookmarkSyncSettingsFragment::class.java, null)
            }
        }
    }
}

