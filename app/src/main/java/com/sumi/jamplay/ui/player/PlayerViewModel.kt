package com.sumi.jamplay.ui.player

import androidx.annotation.OptIn
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.sumi.jamplay.domain.model.Track
import com.sumi.jamplay.service.MusicPlayerService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@kotlin.OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModel : ViewModel() {

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isShuffleMode = MutableStateFlow(false)
    val isShuffleMode: StateFlow<Boolean> = _isShuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(0)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _vibrantColor = MutableStateFlow(Color(0xFF1E1E1E))
    val vibrantColor: StateFlow<Color> = _vibrantColor.asStateFlow()

    private val _lightVibrantColor = MutableStateFlow(Color(0xFF3E3E3E))
    val lightVibrantColor: StateFlow<Color> = _lightVibrantColor.asStateFlow()

    val playerCommand = MutableSharedFlow<PlayerCommand>(replay = 1)
    private var isBound = false

    sealed class PlayerCommand {
        data class Play(val track: Track, val tracks: List<Track>) : PlayerCommand()
        data object TogglePlay : PlayerCommand()
        data object SkipNext : PlayerCommand()
        data object SkipPrevious : PlayerCommand()
        data class Seek(val position: Long) : PlayerCommand()
        data object ToggleShuffle : PlayerCommand()
        data object ToggleRepeat : PlayerCommand()
    }

    @OptIn(UnstableApi::class)
    fun bindService(service: MusicPlayerService) {
        if (isBound) return
        isBound = true

        // 서비스 상태를 그대로 구독
        viewModelScope.launch {
            service.currentTrack.collect { _currentTrack.value = it }
        }
        viewModelScope.launch {
            service.isPlaying.collect { _isPlaying.value = it }
        }
        viewModelScope.launch {
            service.isShuffleMode.collect { _isShuffleMode.value = it }
        }
        viewModelScope.launch {
            service.repeatMode.collect { _repeatMode.value = it }
        }
        viewModelScope.launch {
            service.currentPosition.collect { _currentPosition.value = it }
        }
        viewModelScope.launch {
            service.duration.collect { _duration.value = it }
        }
        viewModelScope.launch {
            service.vibrantColor.collect { _vibrantColor.value = it }
        }
        viewModelScope.launch {
            service.lightVibrantColor.collect { _lightVibrantColor.value = it }
        }
    }

    fun play(track: Track, tracks: List<Track>) {
        playerCommand.tryEmit(PlayerCommand.Play(track, tracks))
        playerCommand.resetReplayCache()
    }

    fun togglePlayPause() {
        playerCommand.tryEmit(PlayerCommand.TogglePlay)
        playerCommand.resetReplayCache()
    }

    fun skipNext() {
        playerCommand.tryEmit(PlayerCommand.SkipNext)
        playerCommand.resetReplayCache()
    }

    fun skipPrevious() {
        playerCommand.tryEmit(PlayerCommand.SkipPrevious)
        playerCommand.resetReplayCache()
    }

    fun seekTo(position: Long) {
        playerCommand.tryEmit(PlayerCommand.Seek(position))
        playerCommand.resetReplayCache()
    }

    fun toggleShuffle() {
        playerCommand.tryEmit(PlayerCommand.ToggleShuffle)
        playerCommand.resetReplayCache()
    }

    fun toggleRepeat() {
        playerCommand.tryEmit(PlayerCommand.ToggleRepeat)
        playerCommand.resetReplayCache()
    }

    fun updateVibrantColors(vibrant: Color, lightVibrant: Color) {
        _vibrantColor.value = vibrant
        _lightVibrantColor.value = lightVibrant
    }
}