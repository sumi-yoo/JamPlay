package com.sumi.jamplay.ui.playlist

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.sumi.jamplay.R
import com.sumi.jamplay.databinding.FragmentPlaylistBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaylistFragment : Fragment(R.layout.fragment_playlist) {
    private val viewModel: PlaylistViewModel by activityViewModels()
    private var binding: FragmentPlaylistBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val content = FragmentPlaylistBinding.bind(view)
        binding = content
        val favoritesId = getString(R.string.favorites_playlist_name).hashCode().toLong()
        viewModel.setFavoritesId(getString(R.string.favorites_playlist_name))
        val adapter = PlaylistAdapter(
            onClick = { row ->
                if (viewModel.deletePlayListMode.value) {
                    viewModel.setPlaylistDeleted(row.playlist.id, !row.checked)
                } else if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    findNavController().navigate(R.id.playlist_detail, bundleOf("playlistId" to row.playlist.id))
                }
            },
            onChecked = viewModel::setPlaylistDeleted
        )
        content.list.layoutManager = LinearLayoutManager(requireContext())
        content.list.adapter = adapter
        content.toolbar.setTitle(R.string.playlist_title)
        content.toolbar.inflateMenu(R.menu.playlist_actions)
        content.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_add -> {
                    if (childFragmentManager.findFragmentByTag(PlaylistNameDialogFragment.TAG) == null) {
                        PlaylistNameDialogFragment.create().show(childFragmentManager, PlaylistNameDialogFragment.TAG)
                    }
                }
                R.id.action_delete -> {
                    viewModel.clearSelectionPlaylists()
                    viewModel.toggleDeletePlayListMode()
                }
                R.id.action_complete -> viewModel.deleteSelectedPlaylists()
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        val back = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                viewModel.clearSelectionPlaylists()
                viewModel.toggleDeletePlayListMode()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, back)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.playlists, viewModel.deletePlayListMode, viewModel.deletedPlaylists) { playlists, deleting, selected ->
                    back.isEnabled = deleting
                    content.toolbar.menu.findItem(R.id.action_complete).isVisible = deleting
                    content.toolbar.menu.findItem(R.id.action_add).isVisible = !deleting
                    content.toolbar.menu.findItem(R.id.action_delete).isVisible = !deleting
                    playlists.map {
                        PlaylistRow(it, deleting && it.id != favoritesId, it.id in selected, !deleting || it.id != favoritesId)
                    }
                }.collect { rows ->
                    adapter.submitList(rows)
                    content.emptyMessage.isVisible = rows.isEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        binding?.list?.adapter = null
        binding = null
        super.onDestroyView()
    }
}
