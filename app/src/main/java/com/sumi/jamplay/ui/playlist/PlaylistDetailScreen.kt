package com.sumi.jamplay.ui.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.sumi.jamplay.R
import com.sumi.jamplay.domain.model.Track
import com.sumi.jamplay.ui.player.MiniPlayerScreen
import com.sumi.jamplay.ui.player.PlayingWave
import com.sumi.jamplay.ui.player.PlayerViewModel
import com.sumi.jamplay.ui.theme.JamPlayPurple

@Composable
fun PlaylistDetailScreen(
    playlistViewModel: PlaylistViewModel,
    playerViewModel: PlayerViewModel,
    onTrackClick: (Track, List<Track>) -> Unit,
    onMiniPlayerClick: () -> Unit,
    onBack: () -> Unit
) {
    val playlist by playlistViewModel.selectedPlaylist.collectAsState()
    val playlists by playlistViewModel.playlists.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val tracks by playlistViewModel.tracks.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val playlistId by playlistViewModel.playlistId.collectAsState()
    val favoritesPlaylistId = stringResource(R.string.favorites_playlist_name).hashCode().toLong()

    if (playlist == null) return

    val lifecycleOwner = LocalLifecycleOwner.current
    var expanded by rememberSaveable { mutableStateOf(false) }

    // 화면 라이프사이클 기반 클릭 제어
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> playlistViewModel.enableClicks()
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> playlistViewModel.disableClicks()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler {
        playlistViewModel.clearSelectionTracks()
        playlistViewModel.updateDeleteTrackMode(false)
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        // 상단 앱바
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    playlistViewModel.clearSelectionTracks()
                    playlistViewModel.updateDeleteTrackMode(false)
                    onBack()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = playlist!!.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (playlistViewModel.deleteTrackMode) {
                TextButton(onClick = {
                    val toDelete = playlistViewModel.deletedTracks.filter { it.value }.keys
                    toDelete.forEach { trackId ->
                        tracks.find { it.id == trackId }?.let {
                            playlistViewModel.deleteTrackFromPlaylist(track = it)
                        }
                    }
                    playlistViewModel.clearSelectionTracks()
                    playlistViewModel.updateDeleteTrackMode(false)
                }) {
                    Text(stringResource(R.string.complete))
                }
            } else {
                // 드롭다운 메뉴
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        offset = DpOffset(x = (-5).dp, y = 0.dp)
                    ) {
                        if (!favoritesPlaylistId.equals(playlistId)) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.rename),
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    playlistViewModel.updateShowCreateDialog(true)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.track_delete), color = Color.White) },
                            onClick = {
                                expanded = false
                                playlistViewModel.updateDeleteTrackMode(true)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = if (currentTrack != null) 0.dp else 16.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_tracks_in_playlist),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = if (currentTrack != null) 0.dp else 16.dp
                    )
            ) {
                items(tracks) { track ->
                    val isCurrentTrack = currentTrack?.id == track.id
                    val isSelected = playlistViewModel.deletedTracks[track.id] ?: false

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable {
                                if (playlistViewModel.deleteTrackMode) {
                                    playlistViewModel.deletedTracks[track.id] = !(playlistViewModel.deletedTracks[track.id] ?: false)
                                } else if (playlistViewModel.acceptsClicks) {
                                    onTrackClick(track, tracks)
                                }
                            }
                        ,
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            contentColor = Color.White          // 내부 Text, Icon 색 기본값
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(64.dp)) {
                                AsyncImage(
                                    model = track.artworkUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))
                                )

                                if (isCurrentTrack) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(Color.Black.copy(alpha = 0.7f))
                                    )
                                    // 재생중 표시
                                    PlayingWave(
                                        isPlaying = isPlaying,
                                        barCount = 5,
                                        barWidth = 3.dp,
                                        barMaxHeight = 24.dp,
                                        barMinHeight = 6.dp,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val animatedColor by animateColorAsState(
                                    targetValue = if (isCurrentTrack) JamPlayPurple else Color.White,
                                    animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)
                                )

                                Text(
                                    text = track.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = animatedColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = if (isCurrentTrack) Modifier.shadow(8.dp, spotColor = JamPlayPurple.copy(alpha = 0.8f)) else Modifier
                                )
                                Text(
                                    text = track.artistName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (playlistViewModel.deleteTrackMode) {
                                Checkbox(
                                    modifier = Modifier
                                        .padding(5.dp)
                                        .size(20.dp),
                                    checked = isSelected,
                                    onCheckedChange = {
                                        playlistViewModel.deletedTracks[track.id] = it
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (currentTrack != null) {
            MiniPlayerScreen(
                viewModel = playerViewModel,
                onClick = {
                    onMiniPlayerClick()
                }
            )
        }
    }

    // 새 플레이리스트 생성 다이얼로그
    if (playlistViewModel.showCreateDialog) {
        CreatePlaylistDialog(
            title = stringResource(R.string.playlist_rename),
            initialName = playlist?.name ?: "",
            existingNames = playlists.map { it.name },
            onConfirm = { newName ->
                playlistViewModel.renamePlaylist(newName)
            },
            onDismiss = {
                playlistViewModel.updateShowCreateDialog(false)
                playlistViewModel.updateNewPlaylistName("")
            }
        )
    }
}