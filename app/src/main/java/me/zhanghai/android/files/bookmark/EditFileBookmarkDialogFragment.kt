/*
 * Copyright (c) 2024 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.bookmark

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.parcelize.Parcelize
import me.zhanghai.android.files.R
import me.zhanghai.android.files.databinding.EditFileBookmarkDialogBinding
import me.zhanghai.android.files.util.ParcelableArgs
import me.zhanghai.android.files.util.args
import me.zhanghai.android.files.util.layoutInflater
import me.zhanghai.android.files.util.setTextWithSelection
import me.zhanghai.android.files.util.show

class EditFileBookmarkDialogFragment : AppCompatDialogFragment() {
    private val args by args<Args>()

    private lateinit var binding: EditFileBookmarkDialogBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(R.string.file_bookmark_edit)
            .apply {
                binding = EditFileBookmarkDialogBinding.inflate(context.layoutInflater)
                val bookmark = args.bookmark

                if (savedInstanceState == null) {
                    binding.nameEdit.setTextWithSelection(bookmark.name)
                    binding.tagsEdit.setTextWithSelection(bookmark.tags.joinToString(", "))
                    binding.notesEdit.setTextWithSelection(bookmark.notes ?: "")
                    binding.showInSidebarSwitch.isChecked = bookmark.showInSidebar
                }

                // Only show sidebar switch for directories
                binding.showInSidebarSwitch.isVisible = bookmark.isDirectory

                setView(binding.root)
            }
            .setPositiveButton(android.R.string.ok) { _, _ -> save() }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
            .setNeutralButton(R.string.remove) { _, _ -> remove() }
            .create()
            .apply {
                window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            }

    private fun save() {
        val name = binding.nameEdit.text.toString().takeIf { it.isNotEmpty() }
            ?: args.bookmark.name
        val tags = binding.tagsEdit.text.toString()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val notes = binding.notesEdit.text.toString().takeIf { it.isNotEmpty() }
        val showInSidebar = binding.showInSidebarSwitch.isChecked

        BookmarkManager.updateBookmark(
            id = args.bookmark.id,
            name = name,
            tags = tags,
            notes = notes,
            showInSidebar = showInSidebar
        )
        dismiss()
    }

    private fun remove() {
        BookmarkManager.deleteBookmark(args.bookmark.id)
        dismiss()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        dismiss()
    }

    @Parcelize
    class Args(val bookmark: FileBookmark) : ParcelableArgs

    companion object {
        fun show(bookmark: FileBookmark, fragment: Fragment) {
            EditFileBookmarkDialogFragment().apply {
                putArgs(Args(bookmark))
                show(fragment)
            }
        }

        private fun EditFileBookmarkDialogFragment.putArgs(args: Args) {
            arguments = args.toBundle()
        }

        private fun Args.toBundle(): Bundle = Bundle().apply {
            putParcelable(EditFileBookmarkDialogFragment::class.java.name, this@toBundle)
        }
    }
}

