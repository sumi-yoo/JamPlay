package com.sumi.jamplay.ui.player

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumi.jamplay.domain.model.Track
import com.sumi.jamplay.domain.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController
) : ViewModel() {
    val currentTrack = playbackController.currentTrack
    val isPlaying = playbackController.isPlaying
    val isShuffleMode = playbackController.isShuffleMode
    val repeatMode = playbackController.repeatMode
    val currentPosition = playbackController.currentPosition
    val duration = playbackController.duration
    val vibrantColor = playbackController.vibrantColor.map { Color(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Color(playbackController.vibrantColor.value))
    val lightVibrantColor = playbackController.lightVibrantColor.map { Color(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Color(playbackController.lightVibrantColor.value))

    init {
        playbackController.connect()
    }

    fun play(track: Track, tracks: List<Track>) = playbackController.play(track, tracks)
    fun togglePlayPause() = playbackController.togglePlayPause()
    fun skipNext() = playbackController.skipNext()
    fun skipPrevious() = playbackController.skipPrevious()
    fun seekTo(position: Long) = playbackController.seekTo(position)
    fun toggleShuffle() = playbackController.toggleShuffle()
    fun toggleRepeat() = playbackController.toggleRepeat()

    override fun onCleared() {
        playbackController.disconnect()
        super.onCleared()
    }
}