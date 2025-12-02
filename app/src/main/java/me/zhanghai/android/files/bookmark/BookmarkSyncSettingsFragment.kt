/*
 * Copyright (c) 2024 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.bookmark

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import me.zhanghai.android.files.R
import me.zhanghai.android.files.databinding.BookmarkSyncSettingsFragmentBinding
import me.zhanghai.android.files.settings.Settings
import me.zhanghai.android.files.util.valueCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookmarkSyncSettingsFragment : Fragment() {
    private lateinit var binding: BookmarkSyncSettingsFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BookmarkSyncSettingsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        activity.setTitle(R.string.file_bookmark_sync_settings)

        setupViews()
        loadSettings()
    }

    private fun setupViews() {
        binding.syncEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            Settings.FILE_BOOKMARK_SYNC_ENABLED.putValue(isChecked)
            updateSyncButtonState()
        }

        binding.syncNowButton.setOnClickListener {
            saveSettings()
            performSync()
        }
    }

    private fun loadSettings() {
        binding.syncEnabledSwitch.isChecked = Settings.FILE_BOOKMARK_SYNC_ENABLED.valueCompat
        binding.webdavUrlEdit.setText(Settings.FILE_BOOKMARK_WEBDAV_URL.valueCompat)
        binding.usernameEdit.setText(Settings.FILE_BOOKMARK_WEBDAV_USERNAME.valueCompat)
        binding.passwordEdit.setText(Settings.FILE_BOOKMARK_WEBDAV_PASSWORD.valueCompat)
        binding.syncPathEdit.setText(Settings.FILE_BOOKMARK_SYNC_PATH.valueCompat)

        updateSyncStatus()
        updateSyncButtonState()
    }

    private fun saveSettings() {
        Settings.FILE_BOOKMARK_WEBDAV_URL.putValue(binding.webdavUrlEdit.text.toString().trim())
        Settings.FILE_BOOKMARK_WEBDAV_USERNAME.putValue(binding.usernameEdit.text.toString().trim())
        Settings.FILE_BOOKMARK_WEBDAV_PASSWORD.putValue(binding.passwordEdit.text.toString())
        val syncPath = binding.syncPathEdit.text.toString().trim().ifEmpty { "/bookmarks/" }
        Settings.FILE_BOOKMARK_SYNC_PATH.putValue(syncPath)
    }

    private fun updateSyncStatus() {
        val status = BookmarkSyncEngine.getSyncStatus()
        binding.lastSyncText.text = if (status.lastSyncTime > 0) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            getString(R.string.file_bookmark_sync_success) + ": " +
                dateFormat.format(Date(status.lastSyncTime))
        } else {
            getString(R.string.file_bookmark_list_empty)
        }
    }

    private fun updateSyncButtonState() {
        val enabled = Settings.FILE_BOOKMARK_SYNC_ENABLED.valueCompat
        binding.syncNowButton.isEnabled = enabled
        binding.webdavUrlEdit.isEnabled = enabled
        binding.usernameEdit.isEnabled = enabled
        binding.passwordEdit.isEnabled = enabled
        binding.syncPathEdit.isEnabled = enabled
    }

    private fun performSync() {
        val url = binding.webdavUrlEdit.text.toString().trim()
        if (url.isEmpty()) {
            Toast.makeText(
                requireContext(),
                R.string.file_bookmark_webdav_url,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        binding.syncNowButton.isEnabled = false
        binding.syncNowButton.text = getString(R.string.loading)

        BookmarkSyncEngine.sync { success, message ->
            activity?.runOnUiThread {
                binding.syncNowButton.isEnabled = true
                binding.syncNowButton.text = getString(R.string.file_bookmark_sync)
                updateSyncStatus()

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
}
