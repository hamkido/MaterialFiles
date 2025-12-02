/*
 * Copyright (c) 2024 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.bookmark

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@Parcelize
data class FileBookmark(
    val id: String,
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val tags: List<String>,
    val notes: String?,
    val createdAt: Long,
    val modifiedAt: Long,
    val showInSidebar: Boolean,
    val isDeleted: Boolean = false
) : Parcelable {

    constructor(
        name: String,
        path: String,
        isDirectory: Boolean,
        tags: List<String> = emptyList(),
        notes: String? = null,
        showInSidebar: Boolean = false
    ) : this(
        id = UUID.randomUUID().toString(),
        name = name,
        path = path,
        isDirectory = isDirectory,
        tags = tags,
        notes = notes,
        createdAt = System.currentTimeMillis(),
        modifiedAt = System.currentTimeMillis(),
        showInSidebar = showInSidebar,
        isDeleted = false
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put(KEY_ID, id)
        put(KEY_NAME, name)
        put(KEY_PATH, path)
        put(KEY_IS_DIRECTORY, isDirectory)
        put(KEY_TAGS, JSONArray(tags))
        put(KEY_NOTES, notes)
        put(KEY_CREATED_AT, createdAt)
        put(KEY_MODIFIED_AT, modifiedAt)
        put(KEY_SHOW_IN_SIDEBAR, showInSidebar)
        put(KEY_IS_DELETED, isDeleted)
    }

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_PATH = "path"
        private const val KEY_IS_DIRECTORY = "isDirectory"
        private const val KEY_TAGS = "tags"
        private const val KEY_NOTES = "notes"
        private const val KEY_CREATED_AT = "createdAt"
        private const val KEY_MODIFIED_AT = "modifiedAt"
        private const val KEY_SHOW_IN_SIDEBAR = "showInSidebar"
        private const val KEY_IS_DELETED = "isDeleted"

        fun fromJson(json: JSONObject): FileBookmark {
            val tagsArray = json.optJSONArray(KEY_TAGS)
            val tags = mutableListOf<String>()
            if (tagsArray != null) {
                for (i in 0 until tagsArray.length()) {
                    tags.add(tagsArray.getString(i))
                }
            }
            return FileBookmark(
                id = json.getString(KEY_ID),
                name = json.getString(KEY_NAME),
                path = json.getString(KEY_PATH),
                isDirectory = json.getBoolean(KEY_IS_DIRECTORY),
                tags = tags,
                notes = json.optString(KEY_NOTES).takeIf { it.isNotEmpty() },
                createdAt = json.getLong(KEY_CREATED_AT),
                modifiedAt = json.getLong(KEY_MODIFIED_AT),
                showInSidebar = json.optBoolean(KEY_SHOW_IN_SIDEBAR, false),
                isDeleted = json.optBoolean(KEY_IS_DELETED, false)
            )
        }
    }
}

