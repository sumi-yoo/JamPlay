package com.sumi.jamplay.ui.playlist

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
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
class PlaylistDetailFragment : Fragment(R.layout.fragment_playlist) {
    private val viewModel: PlaylistViewModel by activityViewModels()
    private val playerViewModel: PlayerViewModel by activityViewModels()
    private var binding: FragmentPlaylistBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val content = FragmentPlaylistBinding.bind(view)
        binding = content
        val playlistId = requireArguments().getLong("playlistId")
        val favoritesId = getString(R.string.favorites_playlist_name).hashCode().toLong()
        viewModel.setFavoritesId(getString(R.string.favorites_playlist_name))
        viewModel.setPlaylistId(playlistId)
        val adapter = PlaylistTrackAdapter(
            onClick = { row ->
                if (viewModel.deleteTrackMode.value) {
                    viewModel.setTrackDeleted(row.track.id, !row.checked)
                } else if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    playerViewModel.play(row.track, viewModel.tracks.value)
                    findNavController().navigate(R.id.player)
                }
            },
            onChecked = viewModel::setTrackDeleted
        )
        content.list.layoutManager = LinearLayoutManager(requireContext())
        content.list.adapter = adapter
        content.emptyMessage.setText(R.string.no_tracks_in_playlist)
        content.toolbar.setNavigationIcon(R.drawable.ic_back)
        content.toolbar.setNavigationContentDescription(R.string.back)
        content.toolbar.setNavigationOnClickListener { leave() }
        content.toolbar.inflateMenu(R.menu.playlist_actions)
        content.toolbar.menu.findItem(R.id.action_add).isVisible = false
        content.toolbar.menu.findItem(R.id.action_delete).setTitle(R.string.track_delete)
        content.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_rename -> {
                    val playlist = viewModel.selectedPlaylist.value?.takeIf { it.id == playlistId }
                    if (playlist != null && childFragmentManager.findFragmentByTag(PlaylistNameDialogFragment.TAG) == null) {
                        PlaylistNameDialogFragment.create(playlistId, playlist.name)
                            .show(childFragmentManager, PlaylistNameDialogFragment.TAG)
                    }
                }
                R.id.action_delete -> viewModel.updateDeleteTrackMode(true)
                R.id.action_complete -> viewModel.deleteSelectedTracks()
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = leave()
        })
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedPlaylist.collect { playlist ->
                        content.toolbar.title = playlist?.takeIf { it.id == playlistId }?.name.orEmpty()
                    }
                }
                launch {
                    viewModel.deleteTrackMode.collect { deleting ->
                        content.toolbar.menu.findItem(R.id.action_complete).isVisible = deleting
                        content.toolbar.menu.findItem(R.id.action_delete).isVisible = !deleting
                        content.toolbar.menu.findItem(R.id.action_rename).isVisible = !deleting && playlistId != favoritesId
                    }
                }
                launch {
                    combine(viewModel.tracks, playerViewModel.currentTrack, playerViewModel.isPlaying,
                        viewModel.deleteTrackMode, viewModel.deletedTracks) { tracks, current, playing, deleting, selected ->
                        tracks.map { PlaylistTrackRow(it, it.id == current?.id, playing, deleting, it.id in selected) }
                    }.collect { rows ->
                        adapter.submitList(rows)
                        content.emptyMessage.isVisible = rows.isEmpty()
                    }
                }
            }
        }
    }

    private fun leave() {
        viewModel.clearSelectionTracks()
        viewModel.updateDeleteTrackMode(false)
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        binding?.list?.adapter = null
        binding = null
        super.onDestroyView()
    }
}
