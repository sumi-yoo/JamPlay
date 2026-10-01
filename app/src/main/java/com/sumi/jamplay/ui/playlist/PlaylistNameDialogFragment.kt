package com.sumi.jamplay.ui.playlist

import android.app.Dialog
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sumi.jamplay.R
import com.sumi.jamplay.databinding.DialogPlaylistNameBinding
import com.sumi.jamplay.domain.policy.PlaylistNamePolicy
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlaylistNameDialogFragment : DialogFragment() {
    private val viewModel: PlaylistViewModel by activityViewModels()
    private var binding: DialogPlaylistNameBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val content = DialogPlaylistNameBinding.inflate(layoutInflater)
        binding = content
        val editing = requireArguments().containsKey(PLAYLIST_ID)
        content.nameInput.setText(savedInstanceState?.getString(INPUT) ?: requireArguments().getString(INITIAL_NAME).orEmpty())
        content.nameInput.setSelection(content.nameInput.length())
        content.nameInput.doAfterTextChanged { content.root.error = null }
        return MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_JamPlay_PlaylistDialog)
            .setTitle(if (editing) R.string.playlist_rename else R.string.new_playlist_dialog_title)
            .setView(content.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm, null)
            .create()
    }

    override fun onStart() {
        super.onStart()
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener { submit() }
        binding?.nameInput?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else false
        }
    }

    private fun submit() {
        val content = binding ?: return
        val id = arguments?.takeIf { it.containsKey(PLAYLIST_ID) }?.getLong(PLAYLIST_ID)
        val name = content.nameInput.text?.toString().orEmpty()
        val existingNames = viewModel.playlists.value.filter { it.id != id }.map { it.name }
        when (PlaylistNamePolicy.validate(name, existingNames)) {
            PlaylistNamePolicy.Error.EMPTY -> content.root.error = getString(R.string.playlist_name_empty)
            PlaylistNamePolicy.Error.DUPLICATE -> content.root.error = getString(R.string.playlist_name_exists)
            null -> {
                if (id == null) viewModel.addPlaylist(name) else viewModel.renamePlaylist(name, id)
                dismiss()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(INPUT, binding?.nameInput?.text?.toString())
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "playlist_name"
        private const val PLAYLIST_ID = "playlist_id"
        private const val INITIAL_NAME = "initial_name"
        private const val INPUT = "input"

        fun create(playlistId: Long? = null, initialName: String = "") = PlaylistNameDialogFragment().apply {
            arguments = bundleOf(INITIAL_NAME to initialName).apply {
                if (playlistId != null) putLong(PLAYLIST_ID, playlistId)
            }
        }
    }
}
