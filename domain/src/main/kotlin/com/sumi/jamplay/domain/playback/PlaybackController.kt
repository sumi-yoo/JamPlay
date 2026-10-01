package com.sumi.jamplay.domain.playback

import com.sumi.jamplay.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

interface PlaybackController {
    val currentTrack: StateFlow<Track?>
    val isPlaying: StateFlow<Boolean>
    val isShuffleMode: StateFlow<Boolean>
    val repeatMode: StateFlow<Int>
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>
    val vibrantColor: StateFlow<Int>
    val lightVibrantColor: StateFlow<Int>

    fun connect()
    fun disconnect()
    fun play(track: Track, tracks: List<Track>)
    fun togglePlayPause()
    fun skipNext()
    fun skipPrevious()
    fun seekTo(position: Long)
    fun toggleShuffle()
    fun toggleRepeat()
}
