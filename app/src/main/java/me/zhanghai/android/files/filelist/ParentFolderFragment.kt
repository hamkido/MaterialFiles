/*
 * Copyright (c) 2024 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.filelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import java8.nio.file.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.files.databinding.ParentFolderFragmentBinding
import me.zhanghai.android.files.file.FileItem
import me.zhanghai.android.files.file.loadFileItem
import me.zhanghai.android.files.provider.common.newDirectoryStream
import me.zhanghai.android.files.util.Failure
import me.zhanghai.android.files.util.Loading
import me.zhanghai.android.files.util.Stateful
import me.zhanghai.android.files.util.Success
import me.zhanghai.android.files.util.valueCompat
import java.io.IOException

class ParentFolderFragment : Fragment(), ParentFolderAdapter.Listener {
    private lateinit var binding: ParentFolderFragmentBinding
    private lateinit var adapter: ParentFolderAdapter

    lateinit var listener: Listener

    private val currentPathLiveData = MutableLiveData<Path>()
    private val parentFilesLiveData = MutableLiveData<Stateful<List<FileItem>>>()
    private var loadJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        ParentFolderFragmentBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ParentFolderAdapter(this)
        binding.recyclerView.adapter = adapter

        val viewLifecycleOwner = viewLifecycleOwner

        currentPathLiveData.observe(viewLifecycleOwner) { path ->
            val parentPath = path.parent
            if (parentPath != null) {
                loadParentFiles(parentPath)
            } else {
                adapter.clear()
            }
        }

        parentFilesLiveData.observe(viewLifecycleOwner) { stateful ->
            when (stateful) {
                is Loading -> {
                    // Could show loading indicator
                }
                is Success -> {
                    val files = stateful.value
                        .filter { it.attributes.isDirectory }
                        .sortedBy { it.path.fileName?.toString()?.lowercase() }
                    adapter.replace(files, true)
                    adapter.setCurrentPath(currentPathLiveData.value)
                }
                is Failure -> {
                    adapter.clear()
                }
            }
        }
    }

    private fun loadParentFiles(parentPath: Path) {
        loadJob?.cancel()
        parentFilesLiveData.value = Loading(parentFilesLiveData.value?.value)

        loadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val fileList = mutableListOf<FileItem>()
                parentPath.newDirectoryStream().use { directoryStream ->
                    for (path in directoryStream) {
                        try {
                            fileList.add(path.loadFileItem())
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    parentFilesLiveData.value = Success(fileList)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    parentFilesLiveData.value = Failure(parentFilesLiveData.valueCompat.value, e)
                }
            }
        }
    }

    fun setCurrentPath(path: Path) {
        currentPathLiveData.value = path
    }

    override fun onItemClicked(file: FileItem) {
        listener.navigateTo(file.path)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadJob?.cancel()
    }

    interface Listener {
        fun navigateTo(path: Path)
    }
}
