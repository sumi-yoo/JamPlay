package com.sumi.jamplay.ui.player

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.RoundedCornersTransformation
import com.sumi.jamplay.R
import com.sumi.jamplay.databinding.FragmentPlayerBinding
import com.sumi.jamplay.ui.playlist.PlaylistViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class PlayerFragment : Fragment(R.layout.fragment_player) {
    private val playerViewModel: PlayerViewModel by activityViewModels()
    private val playlistViewModel: PlaylistViewModel by activityViewModels()
    private var binding: FragmentPlayerBinding? = null
    private var seeking = false
    private var seekTrackId: Long? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val content = FragmentPlayerBinding.bind(view)
        binding = content
        val controls = content.controls
        val favoritesId = getString(R.string.favorites_playlist_name).hashCode().toLong()
        val favoriteTracks = playlistViewModel.getTracksOfPlaylist(favoritesId)
        var favorite = false
        content.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        controls.playPause.setOnClickListener { playerViewModel.togglePlayPause() }
        controls.previous.setOnClickListener { playerViewModel.skipPrevious() }
        controls.next.setOnClickListener { playerViewModel.skipNext() }
        controls.shuffle.setOnClickListener { playerViewModel.toggleShuffle() }
        controls.repeat.setOnClickListener { playerViewModel.toggleRepeat() }
        controls.addToPlaylist.setOnClickListener {
            if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                findNavController().navigate(R.id.playlist_select)
            }
        }
        controls.favorite.setOnClickListener {
            val track = playerViewModel.currentTrack.value ?: return@setOnClickListener
            if (favorite) playlistViewModel.deleteTrackFromPlaylist(favoritesId, track)
            else playlistViewModel.addTrackToPlaylist(favoritesId, track)
        }
        controls.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                seeking = true
                seekTrackId = playerViewModel.currentTrack.value?.id
            }
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val position = targetPosition(progress)
                controls.position.text = formatTime(position)
                // 접근성 조작은 터치 시작/종료 콜백 없이 진행 값만 바꿀 수 있다.
                if (!seeking) playerViewModel.seekTo(position)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (seekTrackId != null && seekTrackId == playerViewModel.currentTrack.value?.id) {
                    playerViewModel.seekTo(targetPosition(seekBar.progress))
                }
                seeking = false
                seekTrackId = null
            }
        })
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    playerViewModel.currentTrack.collect { track ->
                        content.emptyMessage.isVisible = track == null
                        content.artwork.isVisible = track != null
                        controls.root.isVisible = track != null
                        controls.trackName.text = track?.name
                        controls.artistName.text = track?.artistName
                        content.artwork.load(track?.artworkUrl) {
                            placeholder(R.drawable.ic_playlist)
                            fallback(R.drawable.ic_playlist)
                            error(R.drawable.ic_playlist)
                            transformations(RoundedCornersTransformation(16 * resources.displayMetrics.density))
                        }
                    }
                }
                launch {
                    playerViewModel.isPlaying.collect { playing ->
                        controls.playPause.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
                        controls.playPause.contentDescription = getString(if (playing) R.string.pause else R.string.play)
                    }
                }
                launch {
                    playerViewModel.isShuffleMode.collect { enabled ->
                        controls.shuffle.setImageResource(if (enabled) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle)
                        controls.shuffle.contentDescription = getString(if (enabled) R.string.shuffle_on else R.string.shuffle_off)
                        controls.shuffle.isSelected = enabled
                    }
                }
                launch {
                    playerViewModel.repeatMode.collect { mode ->
                        val (icon, description) = when (mode) {
                            1 -> R.drawable.ic_repeat_on to R.string.repeat_all
                            2 -> R.drawable.ic_repeat_one_on to R.string.repeat_one
                            else -> R.drawable.ic_repeat to R.string.repeat_off
                        }
                        controls.repeat.setImageResource(icon)
                        controls.repeat.contentDescription = getString(description)
                        controls.repeat.isSelected = mode != 0
                    }
                }
                launch {
                    combine(playerViewModel.currentPosition, playerViewModel.duration) { position, duration -> position to duration }
                        .collect { (position, duration) ->
                            controls.duration.text = formatTime(duration)
                            controls.seekBar.isEnabled = duration > 0
                            if (!seeking) {
                                controls.position.text = formatTime(position)
                                controls.seekBar.progress = if (duration > 0) ((position.toDouble() / duration) * SEEK_MAX).toInt().coerceIn(0, SEEK_MAX) else 0
                            }
                        }
                }
                launch {
                    combine(playerViewModel.currentTrack, favoriteTracks) { track, tracks ->
                        track != null && tracks.any { it.id == track.id }
                    }.collect { selected ->
                        favorite = selected
                        controls.favorite.imageTintList = ColorStateList.valueOf(
                            if (selected) ContextCompat.getColor(requireContext(), R.color.jamplay_purple) else Color.WHITE
                        )
                        controls.favorite.isSelected = selected
                        controls.favorite.contentDescription = getString(if (selected) R.string.favorite_remove else R.string.favorite_add)
                    }
                }

            }
        }
    }

    private fun targetPosition(progress: Int): Long =
        (playerViewModel.duration.value.coerceAtLeast(0) * (progress.toDouble() / SEEK_MAX)).toLong()

    private fun formatTime(milliseconds: Long): String {
        val seconds = milliseconds.coerceAtLeast(0) / 1000
        return String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
    }

    override fun onDestroyView() {
        binding?.controls?.seekBar?.setOnSeekBarChangeListener(null)
        seeking = false
        seekTrackId = null
        binding = null
        super.onDestroyView()
    }

    companion object {
        private const val SEEK_MAX = 1000
    }
}
