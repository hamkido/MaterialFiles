/*
 * Copyright (c) 2024 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.bookmark

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import java.util.UUID

@Parcelize
data class SyncMetadata(
    val lastSyncTime: Long,
    val deviceId: String,
    val version: Int,
    val lastModifiedTime: Long
) : Parcelable {

    fun toJson(): JSONObject = JSONObject().apply {
        put(KEY_LAST_SYNC_TIME, lastSyncTime)
        put(KEY_DEVICE_ID, deviceId)
        put(KEY_VERSION, version)
        put(KEY_LAST_MODIFIED_TIME, lastModifiedTime)
    }

    companion object {
        private const val KEY_LAST_SYNC_TIME = "lastSyncTime"
        private const val KEY_DEVICE_ID = "deviceId"
        private const val KEY_VERSION = "version"
        private const val KEY_LAST_MODIFIED_TIME = "lastModifiedTime"

        const val CURRENT_VERSION = 1

        fun create(): SyncMetadata = SyncMetadata(
            lastSyncTime = 0L,
            deviceId = UUID.randomUUID().toString(),
            version = CURRENT_VERSION,
            lastModifiedTime = System.currentTimeMillis()
        )

        fun fromJson(json: JSONObject): SyncMetadata = SyncMetadata(
            lastSyncTime = json.getLong(KEY_LAST_SYNC_TIME),
            deviceId = json.getString(KEY_DEVICE_ID),
            version = json.getInt(KEY_VERSION),
            lastModifiedTime = json.optLong(KEY_LAST_MODIFIED_TIME, 0L)
        )
    }
}

