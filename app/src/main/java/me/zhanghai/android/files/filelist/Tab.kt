/*
 * Copyright (c) 2024 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.filelist

import android.os.Parcelable
import java8.nio.file.Path
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import me.zhanghai.android.files.util.ParcelableParceler

@Parcelize
data class Tab(
    val id: Long,
    val path: @WriteWith<ParcelableParceler> Path,
    val title: String? = null
) : Parcelable {
    fun getDisplayTitle(): String = title ?: path.fileName?.toString() ?: path.toString()
}

