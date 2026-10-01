package com.sumi.jamplay.ui.playlist

import android.os.Bundle
import android.view.View
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
import com.sumi.jamplay.ui.player.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaylistSelectFragment : Fragment(R.layout.fragment_playlist) {
    private val viewModel: PlaylistViewModel by activityViewModels()
    private val playerViewModel: PlayerViewModel by activityViewModels()
    private var binding: FragmentPlaylistBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) viewModel.clearSelectedPlaylists()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val content = FragmentPlaylistBinding.bind(view)
        binding = content
        viewModel.setFavoritesId(getString(R.string.favorites_playlist_name))
        val adapter = PlaylistAdapter(
            onClick = { viewModel.setPlaylistSelected(it.playlist.id, !it.checked) },
            onChecked = viewModel::setPlaylistSelected
        )
        content.list.layoutManager = LinearLayoutManager(requireContext())
        content.list.adapter = adapter
        content.toolbar.setTitle(R.string.playlist_add_title)
        content.toolbar.setNavigationIcon(R.drawable.ic_back)
        content.toolbar.setNavigationContentDescription(R.string.back)
        content.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        content.toolbar.inflateMenu(R.menu.playlist_actions)
        content.toolbar.menu.findItem(R.id.action_complete).isVisible = true
        content.toolbar.menu.findItem(R.id.action_add).isVisible = false
        content.toolbar.menu.findItem(R.id.action_delete).isVisible = false
        content.toolbar.setOnMenuItemClickListener {
            if (it.itemId != R.id.action_complete) return@setOnMenuItemClickListener false
            playerViewModel.currentTrack.value?.let(viewModel::savePlaylistSelection)
            findNavController().popBackStack()
            true
        }
        content.createPlaylist.isVisible = true
        content.createPlaylist.setOnClickListener {
            if (childFragmentManager.findFragmentByTag(PlaylistNameDialogFragment.TAG) == null) {
                PlaylistNameDialogFragment.create().show(childFragmentManager, PlaylistNameDialogFragment.TAG)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(viewModel.playlists, playerViewModel.currentTrack) { playlists, track -> playlists to track }
                        .collect { (playlists, track) ->
                            content.toolbar.menu.findItem(R.id.action_complete).isEnabled = track != null
                            if (track != null) viewModel.initializeSelection(track, playlists)
                        }
                }
                launch {
                    combine(viewModel.playlists, viewModel.selectedPlaylists) { playlists, selected ->
                        playlists.map { PlaylistRow(it, showCheckbox = true, checked = selected[it.id] == true) }
                    }.collect { rows ->
                        adapter.submitList(rows)
                        content.emptyMessage.isVisible = rows.isEmpty()
                    }
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
