/*
 * Copyright (c) 2024 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.bookmark

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.files.provider.webdav.client.Authority
import me.zhanghai.android.files.provider.webdav.client.PasswordAuthentication
import me.zhanghai.android.files.provider.webdav.client.Protocol
import me.zhanghai.android.files.provider.webdav.createWebDavRootPath
import me.zhanghai.android.files.settings.Settings
import me.zhanghai.android.files.storage.WebDavServer
import me.zhanghai.android.files.storage.WebDavServerAuthenticator
import me.zhanghai.android.files.util.valueCompat
import org.json.JSONArray
import org.json.JSONObject
import java8.nio.file.Files
import java8.nio.file.Path
import java.io.IOException
import java.net.URI
import java.nio.charset.StandardCharsets

/**
 * Sync engine for file bookmarks with WebDAV support.
 * Implements bidirectional sync with conflict detection.
 */
object BookmarkSyncEngine {
    private const val BOOKMARKS_FILE = "bookmarks.json"
    private const val METADATA_FILE = ".sync_metadata.json"
    private const val KEY_BOOKMARKS = "bookmarks"
    private const val AUTO_SYNC_DEBOUNCE_MS = 5000L // 5 seconds debounce for auto sync

    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastAutoSyncTime = 0L
    private var pendingAutoSync = false

    fun sync(callback: (success: Boolean, message: String?) -> Unit) {
        scope.launch {
            try {
                val result = performSync()
                withContext(Dispatchers.Main) {
                    callback(result.success, result.message)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(false, e.message)
                }
            }
        }
    }

    /**
     * Trigger auto sync if enabled. Uses debouncing to avoid too frequent syncs.
     */
    fun triggerAutoSync() {
        val syncEnabled = Settings.FILE_BOOKMARK_SYNC_ENABLED.valueCompat
        val webdavUrl = Settings.FILE_BOOKMARK_WEBDAV_URL.valueCompat.trim()
        
        if (!syncEnabled || webdavUrl.isEmpty()) {
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastAutoSyncTime < AUTO_SYNC_DEBOUNCE_MS) {
            // Schedule a delayed sync if not already pending
            if (!pendingAutoSync) {
                pendingAutoSync = true
                scope.launch {
                    kotlinx.coroutines.delay(AUTO_SYNC_DEBOUNCE_MS)
                    pendingAutoSync = false
                    performAutoSync()
                }
            }
            return
        }

        performAutoSync()
    }

    private fun performAutoSync() {
        lastAutoSyncTime = System.currentTimeMillis()
        scope.launch {
            try {
                performSync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Initialize and perform initial sync on app startup.
     */
    fun initialize() {
        triggerAutoSync()
    }

    private suspend fun performSync(): SyncResult = withContext(Dispatchers.IO) {
        val syncEnabled = Settings.FILE_BOOKMARK_SYNC_ENABLED.valueCompat
        if (!syncEnabled) {
            return@withContext SyncResult(false, "Sync is not enabled")
        }

        val webdavUrl = Settings.FILE_BOOKMARK_WEBDAV_URL.valueCompat.trim()
        if (webdavUrl.isEmpty()) {
            return@withContext SyncResult(false, "WebDAV URL is not configured")
        }

        val username = Settings.FILE_BOOKMARK_WEBDAV_USERNAME.valueCompat
        val password = Settings.FILE_BOOKMARK_WEBDAV_PASSWORD.valueCompat
        val syncPath = Settings.FILE_BOOKMARK_SYNC_PATH.valueCompat.ifEmpty { "/bookmarks/" }

        var transientServer: WebDavServer? = null
        try {
            // Parse WebDAV URL
            val uri = URI(webdavUrl)
            val protocol = when (uri.scheme?.lowercase()) {
                "https", "webdavs", "davs" -> Protocol.DAVS
                "http", "webdav", "dav" -> Protocol.DAV
                else -> return@withContext SyncResult(false, "Invalid WebDAV URL scheme. Use http:// or https://")
            }
            val host = uri.host ?: return@withContext SyncResult(false, "Invalid WebDAV URL host")
            val port = if (uri.port > 0) uri.port else protocol.defaultPort
            val basePath = uri.path?.trimEnd('/') ?: ""

            val authority = Authority(protocol, host, port, username)
            val authentication = PasswordAuthentication(password)

            // Create a transient WebDavServer for authentication
            transientServer = WebDavServer(
                id = null,
                customName = "Bookmark Sync",
                authority = authority,
                authentication = authentication,
                relativePath = basePath.trimStart('/')
            )

            // Add to authenticator
            WebDavServerAuthenticator.addTransientServer(transientServer)

            val remotePath = authority.createWebDavRootPath()
                .resolve(basePath.trimStart('/'))
                .resolve(syncPath.trim('/'))

            // Ensure remote directory exists
            if (!Files.exists(remotePath)) {
                Files.createDirectories(remotePath)
            }

            val remoteBookmarksPath = remotePath.resolve(BOOKMARKS_FILE)
            val remoteMetadataPath = remotePath.resolve(METADATA_FILE)

            // Read remote data
            val remoteData = readRemoteData(remoteBookmarksPath, remoteMetadataPath)
            val localData = LocalData(
                bookmarks = BookmarkRepository.getBookmarks(),
                metadata = BookmarkRepository.getSyncMetadata()
            )

            // Perform sync based on timestamps
            val syncedData = mergeData(localData, remoteData)

            // Save merged data locally
            BookmarkRepository.saveBookmarks(syncedData.bookmarks)
            BookmarkRepository.saveSyncMetadata(syncedData.metadata.copy(
                lastSyncTime = System.currentTimeMillis()
            ))

            // Write to remote
            writeRemoteData(remoteBookmarksPath, remoteMetadataPath, syncedData)

            SyncResult(true, null)
        } catch (e: IOException) {
            e.printStackTrace()
            SyncResult(false, "IO Error: ${e.message}")
        } catch (e: Exception) {
            e.printStackTrace()
            SyncResult(false, "Sync failed: ${e.message}")
        } finally {
            // Remove transient server
            transientServer?.let { WebDavServerAuthenticator.removeTransientServer(it) }
        }
    }

    private fun readRemoteData(bookmarksPath: Path, metadataPath: Path): RemoteData? {
        return try {
            if (!Files.exists(bookmarksPath)) {
                return null
            }

            val bookmarksJson = String(Files.readAllBytes(bookmarksPath), StandardCharsets.UTF_8)
            val jsonObject = JSONObject(bookmarksJson)
            val bookmarksArray = jsonObject.getJSONArray(KEY_BOOKMARKS)
            val bookmarks = mutableListOf<FileBookmark>()
            for (i in 0 until bookmarksArray.length()) {
                bookmarks.add(FileBookmark.fromJson(bookmarksArray.getJSONObject(i)))
            }

            val metadata = if (Files.exists(metadataPath)) {
                val metadataJson = String(Files.readAllBytes(metadataPath), StandardCharsets.UTF_8)
                SyncMetadata.fromJson(JSONObject(metadataJson))
            } else {
                SyncMetadata.create()
            }

            RemoteData(bookmarks, metadata)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun writeRemoteData(bookmarksPath: Path, metadataPath: Path, data: MergedData) {
        // Write bookmarks
        val bookmarksJson = JSONObject().apply {
            val bookmarksArray = JSONArray()
            data.bookmarks.forEach { bookmarksArray.put(it.toJson()) }
            put(KEY_BOOKMARKS, bookmarksArray)
        }
        Files.write(bookmarksPath, bookmarksJson.toString(2).toByteArray(StandardCharsets.UTF_8))

        // Write metadata
        val metadataJson = data.metadata.toJson()
        Files.write(metadataPath, metadataJson.toString(2).toByteArray(StandardCharsets.UTF_8))
    }

    private fun mergeData(local: LocalData, remote: RemoteData?): MergedData {
        if (remote == null) {
            // No remote data, use local
            return MergedData(
                bookmarks = local.bookmarks,
                metadata = local.metadata
            )
        }

        val localBookmarksMap = local.bookmarks.associateBy { it.id }.toMutableMap()
        val mergedBookmarks = mutableListOf<FileBookmark>()

        // Process remote bookmarks
        for (remoteBookmark in remote.bookmarks) {
            val localBookmark = localBookmarksMap.remove(remoteBookmark.id)
            if (localBookmark == null) {
                // New from remote
                if (!remoteBookmark.isDeleted) {
                    mergedBookmarks.add(remoteBookmark)
                }
            } else {
                // Exists in both - resolve conflict by timestamp
                val winner = if (remoteBookmark.modifiedAt > localBookmark.modifiedAt) {
                    remoteBookmark
                } else {
                    localBookmark
                }
                if (!winner.isDeleted) {
                    mergedBookmarks.add(winner)
                }
            }
        }

        // Add remaining local bookmarks (not in remote)
        for (localBookmark in localBookmarksMap.values) {
            if (!localBookmark.isDeleted) {
                mergedBookmarks.add(localBookmark)
            }
        }

        // Clean up deleted bookmarks that are old enough
        val cutoffTime = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L // 30 days
        val cleanedBookmarks = mergedBookmarks.filter {
            !it.isDeleted || it.modifiedAt > cutoffTime
        }

        return MergedData(
            bookmarks = cleanedBookmarks,
            metadata = local.metadata.copy(
                lastModifiedTime = System.currentTimeMillis(),
                version = maxOf(local.metadata.version, remote.metadata.version)
            )
        )
    }

    fun getSyncStatus(): SyncStatus {
        val syncEnabled = Settings.FILE_BOOKMARK_SYNC_ENABLED.valueCompat
        val metadata = BookmarkRepository.getSyncMetadata()
        return SyncStatus(
            enabled = syncEnabled,
            lastSyncTime = metadata.lastSyncTime,
            deviceId = metadata.deviceId
        )
    }

    data class SyncResult(
        val success: Boolean,
        val message: String?
    )

    data class SyncStatus(
        val enabled: Boolean,
        val lastSyncTime: Long,
        val deviceId: String
    )

    private data class LocalData(
        val bookmarks: List<FileBookmark>,
        val metadata: SyncMetadata
    )

    private data class RemoteData(
        val bookmarks: List<FileBookmark>,
        val metadata: SyncMetadata
    )

    private data class MergedData(
        val bookmarks: List<FileBookmark>,
        val metadata: SyncMetadata
    )
}
