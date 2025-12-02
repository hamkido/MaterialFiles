/*
 * Copyright (c) 2024 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.bookmark

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import java8.nio.file.Path

/**
 * Manager for file bookmarks with support for CRUD operations and filtering.
 */
object BookmarkManager {
    private val _filteredBookmarksLiveData = MediatorLiveData<List<FileBookmark>>()
    val bookmarksLiveData: LiveData<List<FileBookmark>>
        get() = BookmarkRepository.bookmarksLiveData

    private var currentFilter: BookmarkFilter = BookmarkFilter.All

    init {
        _filteredBookmarksLiveData.addSource(BookmarkRepository.bookmarksLiveData) { bookmarks ->
            _filteredBookmarksLiveData.value = applyFilter(bookmarks, currentFilter)
        }
    }

    val filteredBookmarksLiveData: LiveData<List<FileBookmark>> = _filteredBookmarksLiveData

    fun getBookmarks(): List<FileBookmark> = BookmarkRepository.getActiveBookmarks()

    fun getBookmarkById(id: String): FileBookmark? = BookmarkRepository.getBookmarkById(id)

    fun addBookmark(
        name: String,
        path: Path,
        isDirectory: Boolean,
        tags: List<String> = emptyList(),
        notes: String? = null,
        showInSidebar: Boolean = false
    ): FileBookmark {
        val bookmark = FileBookmark(
            name = name,
            path = path.toString(),
            isDirectory = isDirectory,
            tags = tags,
            notes = notes,
            showInSidebar = showInSidebar && isDirectory
        )
        BookmarkRepository.addBookmark(bookmark)
        triggerAutoSync()
        return bookmark
    }

    fun addBookmark(bookmark: FileBookmark) {
        BookmarkRepository.addBookmark(bookmark)
        triggerAutoSync()
    }

    fun updateBookmark(
        id: String,
        name: String? = null,
        tags: List<String>? = null,
        notes: String? = null,
        showInSidebar: Boolean? = null
    ) {
        val bookmark = getBookmarkById(id) ?: return
        val updatedBookmark = bookmark.copy(
            name = name ?: bookmark.name,
            tags = tags ?: bookmark.tags,
            notes = notes ?: bookmark.notes,
            showInSidebar = showInSidebar ?: bookmark.showInSidebar,
            modifiedAt = System.currentTimeMillis()
        )
        BookmarkRepository.updateBookmark(updatedBookmark)
        triggerAutoSync()
    }

    fun updateBookmark(bookmark: FileBookmark) {
        BookmarkRepository.updateBookmark(bookmark)
        triggerAutoSync()
    }

    fun deleteBookmark(id: String) {
        BookmarkRepository.deleteBookmark(id)
        triggerAutoSync()
    }

    private fun triggerAutoSync() {
        BookmarkSyncEngine.triggerAutoSync()
    }

    fun isBookmarked(path: Path): Boolean =
        BookmarkRepository.getActiveBookmarks().any { it.path == path.toString() }

    fun isBookmarked(path: String): Boolean =
        BookmarkRepository.getActiveBookmarks().any { it.path == path }

    fun getBookmarkByPath(path: Path): FileBookmark? =
        BookmarkRepository.getActiveBookmarks().find { it.path == path.toString() }

    fun getBookmarkByPath(path: String): FileBookmark? =
        BookmarkRepository.getActiveBookmarks().find { it.path == path }

    fun toggleBookmark(
        name: String,
        path: Path,
        isDirectory: Boolean,
        showInSidebar: Boolean = false
    ): Boolean {
        val existing = getBookmarkByPath(path)
        return if (existing != null) {
            deleteBookmark(existing.id)
            false
        } else {
            addBookmark(name, path, isDirectory, showInSidebar = showInSidebar)
            true
        }
    }

    fun setFilter(filter: BookmarkFilter) {
        currentFilter = filter
        val bookmarks = BookmarkRepository.getBookmarks()
        _filteredBookmarksLiveData.value = applyFilter(bookmarks, filter)
    }

    private fun applyFilter(bookmarks: List<FileBookmark>, filter: BookmarkFilter): List<FileBookmark> {
        val activeBookmarks = bookmarks.filter { !it.isDeleted }
        return when (filter) {
            BookmarkFilter.All -> activeBookmarks
            BookmarkFilter.Files -> activeBookmarks.filter { !it.isDirectory }
            BookmarkFilter.Directories -> activeBookmarks.filter { it.isDirectory }
            is BookmarkFilter.ByTag -> activeBookmarks.filter { filter.tag in it.tags }
        }
    }

    fun getDirectoryBookmarksForSidebar(): List<FileBookmark> =
        BookmarkRepository.getDirectoryBookmarksForSidebar()

    fun getFileBookmarks(): List<FileBookmark> =
        BookmarkRepository.getFileBookmarks()

    fun getAllTags(): Set<String> = BookmarkRepository.getAllTags()

    fun searchBookmarks(query: String): List<FileBookmark> {
        val lowerQuery = query.lowercase()
        return getBookmarks().filter {
            it.name.lowercase().contains(lowerQuery) ||
                it.path.lowercase().contains(lowerQuery) ||
                it.tags.any { tag -> tag.lowercase().contains(lowerQuery) } ||
                it.notes?.lowercase()?.contains(lowerQuery) == true
        }
    }

    fun exportToJson(): String = BookmarkRepository.toExportJson()

    fun importFromJson(json: String): Boolean = BookmarkRepository.importFromJson(json)
}

sealed class BookmarkFilter {
    object All : BookmarkFilter()
    object Files : BookmarkFilter()
    object Directories : BookmarkFilter()
    data class ByTag(val tag: String) : BookmarkFilter()
}

