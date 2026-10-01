package com.sumi.jamplay.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.sumi.jamplay.domain.model.Track
import com.sumi.jamplay.domain.playback.PlaybackController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class ServicePlaybackController @Inject constructor(
    @ApplicationContext private val context: Context
) : PlaybackController {
    private val _currentTrack = MutableStateFlow<Track?>(null)
    override val currentTrack = _currentTrack.asStateFlow()
    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying = _isPlaying.asStateFlow()
    private val _isShuffleMode = MutableStateFlow(false)
    override val isShuffleMode = _isShuffleMode.asStateFlow()
    private val _repeatMode = MutableStateFlow(0)
    override val repeatMode = _repeatMode.asStateFlow()
    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition = _currentPosition.asStateFlow()
    private val _duration = MutableStateFlow(0L)
    override val duration = _duration.asStateFlow()
    private val _vibrantColor = MutableStateFlow(0xFF1E1E1E.toInt())
    override val vibrantColor = _vibrantColor.asStateFlow()
    private val _lightVibrantColor = MutableStateFlow(0xFF3E3E3E.toInt())
    override val lightVibrantColor = _lightVibrantColor.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val commands = Channel<(MusicPlayerService) -> Unit>(Channel.BUFFERED)
    private var serviceJob: Job? = null
    private var boundService: MusicPlayerService? = null
    private var bindingRegistered = false
    private var closed = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (closed || !bindingRegistered) return
            val service = (binder as? MusicPlayerService.LocalBinder)?.getService() ?: return
            if (boundService === service && serviceJob?.isActive == true) return
            clearService()
            boundService = service
            serviceJob = scope.launch {
                launch { service.currentTrack.collect { _currentTrack.value = it } }
                launch { service.isPlaying.collect { _isPlaying.value = it } }
                launch { service.isShuffleMode.collect { _isShuffleMode.value = it } }
                launch { service.repeatMode.collect { _repeatMode.value = it } }
                launch { service.currentPosition.collect { _currentPosition.value = it } }
                launch { service.duration.collect { _duration.value = it } }
                launch { service.vibrantColor.collect { _vibrantColor.value = it } }
                launch { service.lightVibrantColor.collect { _lightVibrantColor.value = it } }
                launch { for (command in commands) command(service) }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // 바인딩 등록은 유지된다. 시스템 재연결 전까지 명령을 큐에 보관한다.
            clearService()
        }

        override fun onBindingDied(name: ComponentName?) {
            clearService()
            unregisterBinding()
            if (!closed) registerBinding()
        }

        override fun onNullBinding(name: ComponentName?) {
            clearService()
            unregisterBinding()
        }
    }

    override fun connect() {
        if (closed || bindingRegistered) return
        context.startService(Intent(context, MusicPlayerService::class.java))
        registerBinding()
    }

    private fun registerBinding() {
        bindingRegistered = context.bindService(
            Intent(context, MusicPlayerService::class.java), connection, Context.BIND_AUTO_CREATE
        )
    }

    private fun unregisterBinding() {
        if (!bindingRegistered) return
        context.unbindService(connection)
        bindingRegistered = false
    }

    private fun clearService() {
        serviceJob?.cancel()
        serviceJob = null
        boundService = null
        _isPlaying.value = false
    }

    override fun disconnect() {
        if (closed) return
        closed = true
        clearService()
        unregisterBinding()
        commands.cancel()
        scope.cancel()
        // 서비스는 정지하지 않는다. 화면 종료 후에도 백그라운드 재생을 유지한다.
    }

    private fun send(command: (MusicPlayerService) -> Unit) {
        if (closed) return
        connect()
        scope.launch { commands.send(command) }
    }

    override fun play(track: Track, tracks: List<Track>) {
        val queue = tracks.toList()
        send { it.play(track, queue) }
    }
    override fun togglePlayPause() = send { it.togglePlayPause() }
    override fun skipNext() = send { it.skipNext() }
    override fun skipPrevious() = send { it.skipPrevious() }
    override fun seekTo(position: Long) = send { it.seekTo(position) }
    override fun toggleShuffle() = send { it.toggleShuffle() }
    override fun toggleRepeat() = send { it.toggleRepeat() }
}
