package com.sumi.jamplay.ui.player

import androidx.annotation.OptIn
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.sumi.jamplay.domain.model.Track
import com.sumi.jamplay.service.MusicPlayerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private val commandChannel = Channel<PlayerCommand>(Channel.BUFFERED)
    val playerCommand = commandChannel.receiveAsFlow()
    private var boundService: MusicPlayerService? = null
    private var serviceStateJob: Job? = null

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
        if (boundService === service && serviceStateJob?.isActive == true) return
        unbindService()
        boundService = service
        serviceStateJob = viewModelScope.launch {
            // 서비스 상태를 그대로 구독
            launch {
                service.currentTrack.collect { _currentTrack.value = it }
            }
            launch {
                service.isPlaying.collect { _isPlaying.value = it }
            }
            launch {
                service.isShuffleMode.collect { _isShuffleMode.value = it }
            }
            launch {
                service.repeatMode.collect { _repeatMode.value = it }
            }
            launch {
                service.currentPosition.collect { _currentPosition.value = it }
            }
            launch {
                service.duration.collect { _duration.value = it }
            }
            launch {
                service.vibrantColor.collect { _vibrantColor.value = it }
            }
            launch {
                service.lightVibrantColor.collect { _lightVibrantColor.value = it }
            }
        }
    }

    fun unbindService() {
        serviceStateJob?.cancel()
        serviceStateJob = null
        boundService = null
    }

    fun play(track: Track, tracks: List<Track>) {
        sendCommand(PlayerCommand.Play(track, tracks))
    }

    fun togglePlayPause() {
        sendCommand(PlayerCommand.TogglePlay)
    }

    fun skipNext() {
        sendCommand(PlayerCommand.SkipNext)
    }

    fun skipPrevious() {
        sendCommand(PlayerCommand.SkipPrevious)
    }

    fun seekTo(position: Long) {
        sendCommand(PlayerCommand.Seek(position))
    }

    fun toggleShuffle() {
        sendCommand(PlayerCommand.ToggleShuffle)
    }

    fun toggleRepeat() {
        sendCommand(PlayerCommand.ToggleRepeat)
    }

    private fun sendCommand(command: PlayerCommand) {
        viewModelScope.launch {
            commandChannel.send(command)
        }
    }

    override fun onCleared() {
        unbindService()
        commandChannel.cancel()
        super.onCleared()
    }

    fun updateVibrantColors(vibrant: Color, lightVibrant: Color) {
        _vibrantColor.value = vibrant
        _lightVibrantColor.value = lightVibrant
    }
}