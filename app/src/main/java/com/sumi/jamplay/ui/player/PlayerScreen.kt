package com.sumi.jamplay.ui.player

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.RepeatOneOn
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.sumi.jamplay.R
import com.sumi.jamplay.ui.playlist.PlaylistViewModel
import com.sumi.jamplay.ui.theme.JamPlayPurple
import com.sumi.jamplay.ui.theme.JamPlayPurpleBright
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PlayerScreen(
    padding: PaddingValues,
    playerViewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel,
    onAddToPlaylist: () -> Unit,
    onBack: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val favoritesPlaylistId = stringResource(R.string.favorites_playlist_name).hashCode().toLong()
    val currentTrack = playerViewModel.currentTrack.collectAsState().value
    val favoriteTracks by playlistViewModel.getTracksOfPlaylist(favoritesPlaylistId).collectAsState(initial = emptyList())
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val isShuffleMode by playerViewModel.isShuffleMode.collectAsState()
    val repeatMode by playerViewModel.repeatMode.collectAsState()
    val currentPosition by playerViewModel.currentPosition.collectAsState()
    val duration by playerViewModel.duration.collectAsState()

    if (currentTrack == null) return

    // 현재 트랙이 즐겨찾기 안에 있는지 실시간으로 판단
    val isFavorite = remember(currentTrack, favoriteTracks) {
        favoriteTracks.any { it.id == currentTrack.id }
    }

    val vibrantColor by playerViewModel.vibrantColor.collectAsState()
    val lightVibrantColor by playerViewModel.lightVibrantColor.collectAsState()
    val contentColor = Color.White

    // 배경 + 오버레이
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(vibrantColor, lightVibrantColor)
                )
            )
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(padding)
    ) {
        // 상단 앱바
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "",
                color = contentColor,
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (isLandscape) {
            // 가로 레이아웃
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 20.dp, start = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtwork(currentTrack.artworkUrl)
                Spacer(modifier = Modifier.width(24.dp))
                // 우측 컨트롤
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentTrack.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center,
                            color = contentColor
                        )
                        Text(
                            text = currentTrack.artistName,
                            fontSize = 18.sp,
                            color = contentColor.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CustomProgressBar(
                            currentPosition = currentPosition,
                            duration = duration,
                            onSeek = { pos -> playerViewModel.seekTo(pos) },
                            activeColor = JamPlayPurple,
                            inactiveColor = Color.White.copy(alpha = 0.25f),
                            thumbColor = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatTime(currentPosition), color = contentColor)
                            Text(formatTime(duration), color = contentColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 재생 컨트롤
                    Row(modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { playerViewModel.toggleShuffle() }) {
                            Icon(
                                if (isShuffleMode) Icons.Filled.ShuffleOn else Icons.Filled.Shuffle,
                                contentDescription = "Shuffle",
                                tint = contentColor
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { playerViewModel.skipPrevious() }) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                modifier = Modifier.size(48.dp),
                                tint = contentColor
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { playerViewModel.togglePlayPause() }) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(48.dp),
                                tint = contentColor
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { playerViewModel.skipNext() }) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(48.dp),
                                tint = contentColor
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { playerViewModel.toggleRepeat() }) {
                            Icon(
                                when (repeatMode) {
                                    1 -> Icons.Filled.RepeatOn
                                    2 -> Icons.Filled.RepeatOneOn
                                    else -> Icons.Filled.Repeat
                                },
                                contentDescription = "Repeat",
                                tint = contentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 좋아요 + 플레이리스트
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = {
                                if (isFavorite) {
                                    playlistViewModel.deleteTrackFromPlaylist(favoritesPlaylistId, currentTrack)
                                } else {
                                    playlistViewModel.addTrackToPlaylist(favoritesPlaylistId, currentTrack)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) JamPlayPurpleBright else Color.White
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { onAddToPlaylist() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Add to Playlist",
                                tint = contentColor
                            )
                        }
                    }
                }
            }

        } else {
            // 세로 레이아웃
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                AlbumArtwork(currentTrack.artworkUrl)
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    currentTrack.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = contentColor
                )
                Text(
                    currentTrack.artistName,
                    fontSize = 18.sp,
                    color = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                CustomProgressBar(
                    currentPosition = currentPosition,
                    duration = duration,
                    onSeek = { pos -> playerViewModel.seekTo(pos) },
                    activeColor = JamPlayPurple,
                    inactiveColor = Color.White.copy(alpha = 0.25f),
                    thumbColor = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(currentPosition), color = contentColor)
                    Text(formatTime(duration), color = contentColor)
                }

                // 재생 컨트롤
                Row(modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { playerViewModel.toggleShuffle() }) {
                        Icon(
                            if (isShuffleMode) Icons.Filled.ShuffleOn else Icons.Filled.Shuffle,
                            contentDescription = "Shuffle",
                            modifier = Modifier.size(24.dp),
                            tint = contentColor
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { playerViewModel.skipPrevious() }) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier.size(48.dp),
                            tint = contentColor
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { playerViewModel.togglePlayPause() }) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(48.dp),
                            tint = contentColor
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { playerViewModel.skipNext() }) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(48.dp),
                            tint = contentColor
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { playerViewModel.toggleRepeat() }) {
                        Icon(
                            if (repeatMode == 1) Icons.Filled.RepeatOn
                            else if (repeatMode == 2) Icons.Filled.RepeatOneOn
                            else Icons.Filled.Repeat,
                            contentDescription = "Repeat",
                            modifier = Modifier.size(24.dp),
                            tint = contentColor
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = {
                            if (isFavorite) {
                                playlistViewModel.deleteTrackFromPlaylist(favoritesPlaylistId, currentTrack)
                            } else {
                                playlistViewModel.addTrackToPlaylist(favoritesPlaylistId, currentTrack)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) JamPlayPurpleBright else Color.White
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = { onAddToPlaylist() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Add to Playlist",
                            modifier = Modifier.size(28.dp),
                            tint = contentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumArtwork(artworkUrl: String?) {
    Box(
        modifier = Modifier.size(250.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(8.dp)
                .graphicsLayer {
                    shadowElevation = 20f
                    shape = RoundedCornerShape(16.dp)
                    clip = false
                    ambientShadowColor = Color.Black.copy(alpha = 0.25f)
                    spotShadowColor = Color.Black.copy(alpha = 0.25f)
                }
        )

        // 앨범 이미지
        if (!artworkUrl.isNullOrBlank()) {
            Image(
                painter = rememberAsyncImagePainter(artworkUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentScale = ContentScale.Crop
            )
        }
    }
}

fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}

@Composable
fun CustomProgressBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    thumbColor: Color
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    var dragProgress by remember { mutableStateOf(progress) }
    var isDragging by remember { mutableStateOf(false) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp) // thumb 드래그 영역 확보
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek((newProgress * duration).toLong())
                    dragProgress = newProgress
                }
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDrag = { change, _ ->
                        dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeek((dragProgress * duration).toLong())
                    }
                )
            }
    ) {
        val barHeight = 4.dp.toPx()
        val radius = barHeight / 2f
        val current = (if (isDragging) dragProgress else progress) * size.width

        // 비활성 바 (배경)
        drawRoundRect(
            color = inactiveColor,
            size = Size(size.width, barHeight),
            cornerRadius = CornerRadius(radius, radius),
            topLeft = Offset(0f, center.y - barHeight / 2f)
        )

        // 활성 바 (진행 부분)
        drawRoundRect(
            color = activeColor,
            size = Size(current, barHeight),
            cornerRadius = CornerRadius(radius, radius),
            topLeft = Offset(0f, center.y - barHeight / 2f)
        )

        // thumb (동그란 포인트)
        drawCircle(
            color = thumbColor,
            radius = 7.dp.toPx(),
            center = Offset(current, center.y)
        )

        if (isDragging) {
            drawCircle(
                color = thumbColor.copy(alpha = 0.25f),
                radius = 14.dp.toPx(),
                center = Offset(current, center.y)
            )
        }
    }
}

/** Palette 추출용 비트맵 로딩 유틸 */
object CoilImageLoader {
    suspend fun getBitmap(context: Context, url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()
        val result = (loader.execute(request) as? SuccessResult)?.drawable
        return (result as? BitmapDrawable)?.bitmap
    }
}

@Composable
fun PlayingWave(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    barCount: Int = 5,
    barWidth: Dp = 4.dp,
    barMaxHeight: Dp = 28.dp,
    barMinHeight: Dp = 6.dp,
    barSpacing: Dp = 2.dp,
    glowColor: Color = JamPlayPurple
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(barSpacing),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier.height(barMaxHeight)
    ) {
        repeat(barCount) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "wave")
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = barMinHeight.value,
                targetValue = barMaxHeight.value,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400 + index * 100, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "heightAnim"
            )

            val height = if (isPlaying) animatedHeight else barMinHeight.value

            val brush = Brush.verticalGradient(
                colors = listOf(
                    glowColor.copy(alpha = 1f),
                    glowColor.copy(alpha = 0.9f)
                )
            )

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(height.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(50),
                        ambientColor = glowColor.copy(alpha = 0.9f),
                        spotColor = glowColor.copy(alpha = 0.9f),
                        clip = false
                    )
                    .background(brush, shape = RoundedCornerShape(50))
            )
        }
    }
}