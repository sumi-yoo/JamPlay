package com.sumi.jamplay.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import com.sumi.jamplay.MainActivity
import com.sumi.jamplay.R
import com.sumi.jamplay.data.datastore.PlayerPreferencesDataStore
import com.sumi.jamplay.domain.model.Track
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@OptIn(UnstableApi::class)
@AndroidEntryPoint
class MusicPlayerService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "music_playback_channel"

        var instance: MusicPlayerService? = null
            private set
    }

    @Inject lateinit var playerPrefs: PlayerPreferencesDataStore

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var mediaSession: MediaSessionCompat

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(this).build().apply {
            addListener(playerListener)
        }
    }
    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncPlaybackState()
            val duration = player.duration.takeIf { it > 0 } ?: 0L
            if (duration != metadataDuration) publishMediaMetadata()
            showForegroundNotification()
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) {
                when (_repeatMode.value) {
                    0 -> { // 반복 없음
                        if (currentIndex < trackList.lastIndex) {
                            skipNext()
                        } else {
                            currentIndex = (currentIndex + 1) % trackList.size
                            finishTrack()
                        }
                    }
                    1 -> { // 전체 반복
                        if (currentIndex < trackList.lastIndex) {
                            skipNext()
                        } else {
                            currentIndex = 0
                            playCurrent()
                        }
                    }
                    2 -> { // 한 곡 반복
                        playCurrent()
                    }
                }
            }
            _duration.value = exoPlayer.duration.takeIf { it > 0 } ?: 0L
        }
    }

    private var trackList: List<Track> = emptyList()
    private var originalTrackList: List<Track> = emptyList()
    private var currentIndex: Int = 0

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

    private var positionUpdateJob: Job? = null
    private var artworkJob: Job? = null
    private var metadataTrack: Track? = null
    private var albumArtBitmap: Bitmap? = null
    private var metadataDuration = 0L
    private val artworkLoader by lazy { ImageLoader(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        serviceScope.launch {
            playerPrefs.shuffleMode.collect { value ->
                _isShuffleMode.value = value
            }
        }
        serviceScope.launch {
            playerPrefs.repeatMode.collect { value ->
                _repeatMode.value = value
            }
        }
        createMediaSession()
        startUpdatingPosition()
    }

    private fun createMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    resumePlayback()
                }
                override fun onPause() {
                    pausePlayback()
                }
                override fun onSkipToNext() {
                    skipNext()
                    showForegroundNotification()
                }
                override fun onSkipToPrevious() {
                    skipPrevious()
                    showForegroundNotification()
                }
                override fun onSeekTo(pos: Long) {
                    seekTo(pos)
                    showForegroundNotification()
                }
                override fun onCustomAction(action: String?, extras: Bundle?) {
                    when (action) {
                        "ACTION_TOGGLE_SHUFFLE" -> {
                            toggleShuffle()
                            showForegroundNotification()
                        }
                        "ACTION_TOGGLE_REPEAT" -> {
                            toggleRepeat()
                            showForegroundNotification()
                        }
                    }
                }
            })
        }
    }

    private fun updatePlaybackState(state: Int) {
        if (_currentTrack.value == null) return

        val shuffleAction = PlaybackStateCompat.CustomAction.Builder(
            "ACTION_TOGGLE_SHUFFLE", "Shuffle",
            if (_isShuffleMode.value) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle
        ).build()

        val repeatAction = PlaybackStateCompat.CustomAction.Builder(
            "ACTION_TOGGLE_REPEAT", "Repeat",
            if (_repeatMode.value == 1) {
                R.drawable.ic_repeat_on
            } else if (_repeatMode.value == 2) {
                R.drawable.ic_repeat_one_on
            } else {
                R.drawable.ic_repeat
            }
        ).build()

        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, exoPlayer.currentPosition, 1.0f)
            .setBufferedPosition(exoPlayer.bufferedPosition)
            .addCustomAction(shuffleAction)
            .addCustomAction(repeatAction)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .build()
        mediaSession.setPlaybackState(playbackState)
    }

    private fun showForegroundNotification() {
        val track = _currentTrack.value ?: return

        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("navigate_to_player", true) // 클릭 시 PlayerScreen으로 이동
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val shuffleAction = NotificationCompat.Action(
            if (_isShuffleMode.value) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle,
            "Shuffle",
            PendingIntent.getService(
                this, 0,
                Intent(this, MusicPlayerService::class.java).apply { action = "ACTION_TOGGLE_SHUFFLE" },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        val repeatAction = NotificationCompat.Action(
            if (_repeatMode.value == 1) {
                R.drawable.ic_repeat_on
            } else if (_repeatMode.value == 2) {
                R.drawable.ic_repeat_one_on
            } else {
                R.drawable.ic_repeat
            },
            "Repeat",
            PendingIntent.getService(
                this, 1,
                Intent(this, MusicPlayerService::class.java).apply { action = "ACTION_TOGGLE_REPEAT" },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        val deleteIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, MusicPlayerService::class.java).apply {
                action = "ACTION_STOP_SERVICE"
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this@MusicPlayerService, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(track.name)
            .setContentText(track.artistName)
            .setContentIntent(contentIntent)
            // 버튼 추가 순서 = 화면 표시 순서
            .addAction(shuffleAction)   // 0
            .addAction(repeatAction)    // 1
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                     .setShowActionsInCompactView(0, 1)
            )
            .setDeleteIntent(deleteIntent)
            .setOngoing(exoPlayer.isPlaying)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startUpdatingPosition() {
        positionUpdateJob?.cancel()
        positionUpdateJob = serviceScope.launch {
            while (isActive) {
                _currentPosition.value = exoPlayer.currentPosition
                _duration.value = exoPlayer.duration.takeIf { it > 0 } ?: 0L

                syncPlaybackState()

                delay(200)
            }
        }
    }

    private fun updateTrackMetadata(track: Track) {
        // 한 곡 반복이나 같은 곡 재시작 시 이미지를 다시 읽지 않는다.
        if (metadataTrack == track) {
            publishMediaMetadata()
            return
        }
        artworkJob?.cancel()
        metadataTrack = track
        albumArtBitmap = null
        _vibrantColor.value = Color(0xFF1E1E1E)
        _lightVibrantColor.value = Color(0xFF3E3E3E)
        publishMediaMetadata() // 제목은 이미지 로딩을 기다리지 않고 먼저 갱신한다.

        artworkJob = serviceScope.launch {
            val bitmap = track.artworkUrl?.let { loadAlbumArtBitmap(it) }
            if (!isActive || _currentTrack.value != track) return@launch
            albumArtBitmap = bitmap
            publishMediaMetadata()

            val palette = withContext(Dispatchers.Default) {
                bitmap?.let { Palette.from(it).generate() }
            }
            if (!isActive || _currentTrack.value != track) return@launch
            _vibrantColor.value = Color(palette?.vibrantSwatch?.rgb ?: 0xFF1E1E1E.toInt())
            _lightVibrantColor.value = Color(palette?.lightVibrantSwatch?.rgb ?: 0xFF3E3E3E.toInt())
        }
    }

    private fun publishMediaMetadata() {
        val track = metadataTrack ?: return
        if (_currentTrack.value != track) return
        metadataDuration = exoPlayer.duration.takeIf { it > 0 } ?: 0L
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.name)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artistName)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, metadataDuration)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArtBitmap)
                .build()
        )
    }

    fun play(track: Track, tracks: List<Track>) {
        trackList = tracks
        originalTrackList = tracks
        currentIndex = tracks.indexOf(track).takeIf { it >= 0 } ?: 0

        if (_isShuffleMode.value) {
            val current = trackList.firstOrNull { it.id == track.id } ?: return
            val shuffled = trackList.filterNot { it.id == track.id }.shuffled()
            trackList = buildList {
                add(current) // 현재곡을 0번째 위치에 둠
                addAll(shuffled)
            }
            currentIndex = 0
        }

        if (_currentTrack.value?.id != track.id) {
            playCurrent()
        }
    }

    private fun playCurrent() {
        val track = trackList.getOrNull(currentIndex) ?: return
        if (positionUpdateJob == null) startUpdatingPosition()

        exoPlayer.stop()
        exoPlayer.setMediaItem(MediaItem.fromUri(track.streamUrl))
        exoPlayer.prepare()
        exoPlayer.play()
        _currentTrack.value = track
        updateTrackMetadata(track)

        syncPlaybackState()
        showForegroundNotification()
    }

    fun togglePlayPause() {
        if (exoPlayer.playWhenReady && exoPlayer.playerError == null) {
            pausePlayback()
        } else {
            resumePlayback()
        }
    }

    private fun resumePlayback() {
        if (exoPlayer.mediaItemCount == 0) return
        if (exoPlayer.playbackState == Player.STATE_IDLE) {
            exoPlayer.prepare()
        } else if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0)
        }
        exoPlayer.play()
    }

    private fun pausePlayback() {
        exoPlayer.pause()
    }

    private fun syncPlaybackState() {
        val state = exoPlayer.playbackState
        // 버퍼링 중에도 정지 버튼을 제공하고, 오류·종료 시에는 재생 버튼을 표시한다.
        _isPlaying.value = exoPlayer.playerError == null && exoPlayer.playWhenReady &&
            (state == Player.STATE_BUFFERING || state == Player.STATE_READY)

        val sessionState = when {
            exoPlayer.playerError != null -> PlaybackStateCompat.STATE_ERROR
            state == Player.STATE_IDLE || state == Player.STATE_ENDED -> PlaybackStateCompat.STATE_STOPPED
            !exoPlayer.playWhenReady -> PlaybackStateCompat.STATE_PAUSED
            state == Player.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
            exoPlayer.isPlaying -> PlaybackStateCompat.STATE_PLAYING
            else -> PlaybackStateCompat.STATE_PAUSED
        }
        updatePlaybackState(sessionState)
    }

    fun skipNext() {
        if (trackList.isEmpty()) return
        currentIndex = (currentIndex + 1) % trackList.size
        playCurrent()
    }

    fun skipPrevious() {
        if (trackList.isEmpty()) return
        currentIndex = if (currentIndex - 1 < 0) trackList.size - 1 else currentIndex - 1
        playCurrent()
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        syncPlaybackState()
    }

    private fun finishTrack() {
        val track = trackList.getOrNull(currentIndex) ?: return
        exoPlayer.stop()
        exoPlayer.setMediaItem(MediaItem.fromUri(track.streamUrl))
        exoPlayer.prepare()
        exoPlayer.pause()
        _currentTrack.value = track
        updateTrackMetadata(track)

        syncPlaybackState()
        showForegroundNotification()
    }

    fun toggleShuffle() {
        if (trackList.isEmpty()) return

        val currentTrack = _currentTrack.value ?: return

        if (_isShuffleMode.value) {
            // 셔플 해제: 원래 순서로 복원
            val currentId = currentTrack.id
            trackList = originalTrackList
            currentIndex = trackList.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
            _isShuffleMode.value = false
            serviceScope.launch {
                playerPrefs.setShuffleMode(false)
            }
        } else {
            // 셔플 켜기: 현재 트랙 유지하고 나머지 무작위로 섞기
            originalTrackList = trackList // 원본 저장
            val currentId = currentTrack.id
            val current = trackList.firstOrNull { it.id == currentId } ?: return

            val shuffled = trackList.filterNot { it.id == currentId }.shuffled()
            trackList = buildList {
                add(current) // 현재곡을 0번째 위치에 둠
                addAll(shuffled)
            }
            currentIndex = 0
            _isShuffleMode.value = true
            serviceScope.launch {
                playerPrefs.setShuffleMode(true)
            }
        }
        showForegroundNotification()
    }

    fun toggleRepeat() {
        val nextMode = when (_repeatMode.value) {
            0 -> 1
            1 -> 2
            2 -> 0
            else -> 0
        }
        _repeatMode.value = nextMode
        serviceScope.launch { playerPrefs.setRepeatMode(nextMode) }
        showForegroundNotification()
    }

    private suspend fun loadAlbumArtBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(applicationContext)
                .data(url)
                .allowHardware(false)
                .build()
            val result = artworkLoader.execute(request)
            (result.drawable as? BitmapDrawable)?.bitmap
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private fun stopServiceSafely() {
        Log.d("MusicPlayerService", "Stopping service safely")
        artworkJob?.cancel()

        try {
            // ExoPlayer 정리
            try {
                exoPlayer.removeListener(playerListener)
                exoPlayer.release()
            } catch (e: Exception) {
                Log.e("MusicPlayerService", "ExoPlayer release failed: ${e.message}")
            }

            // MediaSession 정리
            try {
                mediaSession.apply {
                    isActive = false
                    setCallback(null)
                    release()
                }
            } catch (e: Exception) {
                Log.e("MusicPlayerService", "MediaSession release failed: ${e.message}")
            }

            // 포그라운드 종료 + 알림 제거
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(NOTIFICATION_ID)
            } catch (e: Exception) {
                Log.e("MusicPlayerService", "Notification cleanup failed: ${e.message}")
            }

            // CoroutineScope 취소
            try {
                serviceScope.cancel()
            } catch (e: Exception) {
                Log.e("MusicPlayerService", "ServiceScope cancel failed: ${e.message}")
            }

        } finally {
            // 인스턴스 초기화 + 서비스 종료
            instance = null
            positionUpdateJob?.cancel()
            stopSelf()
            Log.d("MusicPlayerService", "Service stopped safely")
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        stopServiceSafely()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_TOGGLE_SHUFFLE" -> toggleShuffle()
            "ACTION_TOGGLE_REPEAT" -> toggleRepeat()
            "ACTION_STOP_SERVICE" -> {
                positionUpdateJob?.cancel()
                stopSelf()
            }
        }
        return START_STICKY
    }

}