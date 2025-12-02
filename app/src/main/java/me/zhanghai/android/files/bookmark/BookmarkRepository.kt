/*
 * Copyright (c) 2024 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.bookmark

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import me.zhanghai.android.files.app.application
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local repository for storing bookmarks using SharedPreferences with JSON format.
 */
object BookmarkRepository {
    private const val PREFS_NAME = "file_bookmarks"
    private const val KEY_BOOKMARKS = "bookmarks"
    private const val KEY_SYNC_METADATA = "sync_metadata"

    private val sharedPreferences: SharedPreferences by lazy {
        application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _bookmarksLiveData = MutableLiveData<List<FileBookmark>>()
    val bookmarksLiveData: LiveData<List<FileBookmark>> = _bookmarksLiveData

    private val _syncMetadataLiveData = MutableLiveData<SyncMetadata>()
    val syncMetadataLiveData: LiveData<SyncMetadata> = _syncMetadataLiveData

    init {
        loadBookmarks()
        loadSyncMetadata()
    }

    private fun loadBookmarks() {
        val json = sharedPreferences.getString(KEY_BOOKMARKS, null)
        val bookmarks = if (json != null) {
            parseBookmarksJson(json)
        } else {
            emptyList()
        }
        // Use postValue to allow initialization from background thread
        _bookmarksLiveData.postValue(bookmarks)
    }

    private fun loadSyncMetadata() {
        val json = sharedPreferences.getString(KEY_SYNC_METADATA, null)
        val metadata = if (json != null) {
            try {
                SyncMetadata.fromJson(JSONObject(json))
            } catch (e: Exception) {
                SyncMetadata.create()
            }
        } else {
            SyncMetadata.create()
        }
        // Use postValue to allow initialization from background thread
        _syncMetadataLiveData.postValue(metadata)
    }

    fun getBookmarks(): List<FileBookmark> = _bookmarksLiveData.value ?: emptyList()

    fun getActiveBookmarks(): List<FileBookmark> = getBookmarks().filter { !it.isDeleted }

    fun getSyncMetadata(): SyncMetadata = _syncMetadataLiveData.value ?: SyncMetadata.create()

    fun getBookmarkById(id: String): FileBookmark? = getBookmarks().find { it.id == id }

    fun addBookmark(bookmark: FileBookmark) {
        val bookmarks = getBookmarks().toMutableList()
        bookmarks.add(bookmark)
        saveBookmarks(bookmarks)
    }

    fun updateBookmark(bookmark: FileBookmark) {
        val bookmarks = getBookmarks().toMutableList()
        val index = bookmarks.indexOfFirst { it.id == bookmark.id }
        if (index != -1) {
            bookmarks[index] = bookmark.copy(modifiedAt = System.currentTimeMillis())
            saveBookmarks(bookmarks)
        }
    }

    fun deleteBookmark(id: String, hardDelete: Boolean = false) {
        val bookmarks = getBookmarks().toMutableList()
        if (hardDelete) {
            bookmarks.removeAll { it.id == id }
        } else {
            val index = bookmarks.indexOfFirst { it.id == id }
            if (index != -1) {
                bookmarks[index] = bookmarks[index].copy(
                    isDeleted = true,
                    modifiedAt = System.currentTimeMillis()
                )
            }
        }
        saveBookmarks(bookmarks)
    }

    fun saveBookmarks(bookmarks: List<FileBookmark>) {
        val jsonArray = JSONArray()
        bookmarks.forEach { jsonArray.put(it.toJson()) }
        sharedPreferences.edit()
            .putString(KEY_BOOKMARKS, jsonArray.toString())
            .apply()
        // Use postValue to allow calling from background thread
        _bookmarksLiveData.postValue(bookmarks)
        updateModifiedTime()
    }

    fun saveSyncMetadata(metadata: SyncMetadata) {
        sharedPreferences.edit()
            .putString(KEY_SYNC_METADATA, metadata.toJson().toString())
            .apply()
        // Use postValue to allow calling from background thread
        _syncMetadataLiveData.postValue(metadata)
    }

    private fun updateModifiedTime() {
        val metadata = getSyncMetadata().copy(lastModifiedTime = System.currentTimeMillis())
        saveSyncMetadata(metadata)
    }

    fun toExportJson(): String {
        val jsonObject = JSONObject()
        val bookmarksArray = JSONArray()
        getBookmarks().forEach { bookmarksArray.put(it.toJson()) }
        jsonObject.put(KEY_BOOKMARKS, bookmarksArray)
        jsonObject.put(KEY_SYNC_METADATA, getSyncMetadata().toJson())
        return jsonObject.toString(2)
    }

    fun importFromJson(json: String): Boolean {
        return try {
            val jsonObject = JSONObject(json)
            val bookmarksArray = jsonObject.getJSONArray(KEY_BOOKMARKS)
            val bookmarks = mutableListOf<FileBookmark>()
            for (i in 0 until bookmarksArray.length()) {
                bookmarks.add(FileBookmark.fromJson(bookmarksArray.getJSONObject(i)))
            }
            saveBookmarks(bookmarks)

            val metadataJson = jsonObject.optJSONObject(KEY_SYNC_METADATA)
            if (metadataJson != null) {
                saveSyncMetadata(SyncMetadata.fromJson(metadataJson))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun parseBookmarksJson(json: String): List<FileBookmark> {
        return try {
            val jsonArray = JSONArray(json)
            val bookmarks = mutableListOf<FileBookmark>()
            for (i in 0 until jsonArray.length()) {
                bookmarks.add(FileBookmark.fromJson(jsonArray.getJSONObject(i)))
            }
            bookmarks
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getDirectoryBookmarksForSidebar(): List<FileBookmark> =
        getActiveBookmarks().filter { it.isDirectory && it.showInSidebar }

    fun getFileBookmarks(): List<FileBookmark> =
        getActiveBookmarks().filter { !it.isDirectory }

    fun getBookmarksByTag(tag: String): List<FileBookmark> =
        getActiveBookmarks().filter { tag in it.tags }

    fun getAllTags(): Set<String> =
        getActiveBookmarks().flatMap { it.tags }.toSet()
}

