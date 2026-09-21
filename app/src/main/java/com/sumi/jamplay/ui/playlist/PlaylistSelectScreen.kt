package com.sumi.jamplay.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.sumi.jamplay.R
import com.sumi.jamplay.ui.player.PlayerViewModel

@Composable
fun PlaylistSelectScreen(
    padding: PaddingValues,
    playlistViewModel: PlaylistViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val playlists by playlistViewModel.playlists.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    var initialized by rememberSaveable { mutableStateOf(false) }

    // 체크 상태 초기화
    LaunchedEffect(playlists, currentTrack) {
        if (!initialized) {
            playlists.forEach { playlist ->
                val alreadyIn = currentTrack?.let { playlistViewModel.isTrackInPlaylist(playlist.id, it.id) } ?: false
                playlistViewModel.selectedPlaylists[playlist.id] = alreadyIn
            }
            initialized = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
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

            Text(
                text = stringResource(R.string.playlist_add_title),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            TextButton(onClick = {
                currentTrack?.let { track ->
                    playlistViewModel.savePlaylistSelection(track)
                }
                onBack()
                playlistViewModel.clearSelectedPlaylists()
            }) {
                Text(stringResource(R.string.complete))
            }
        }

        Spacer(modifier = Modifier.padding(4.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(end = 16.dp, start = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (playlists.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_playlists),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                item {
                    Text(
                        stringResource(R.string.select_playlist),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(12.dp))
                }

                items(playlists) { playlist ->
                    val checked = playlistViewModel.selectedPlaylists[playlist.id] ?: false
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playlistViewModel.selectedPlaylists[playlist.id] = !(playlistViewModel.selectedPlaylists[playlist.id] ?: false)
                            },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            contentColor = Color.White          // 내부 Text, Icon 색 기본값
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(
                                        R.string.playlist_track_count,
                                        playlist.tracks.size
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            Checkbox(
                                modifier = Modifier
                                    .padding(5.dp)
                                    .size(20.dp),
                                checked = checked,
                                onCheckedChange = {
                                    playlistViewModel.selectedPlaylists[playlist.id] = it
                                }
                            )
                        }
                    }
                }
            }

            // 새 플레이리스트 버튼
            item {
                Spacer(Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .background(Color(0xFF1C1C1C))
                        .clickable { playlistViewModel.updateShowCreateDialog(true) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.create_new_playlist),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // 새 플레이리스트 생성 다이얼로그
    if (playlistViewModel.showCreateDialog) {
        CreatePlaylistDialog(
            title = stringResource(R.string.new_playlist_dialog_title),
            existingNames = playlists.map { it.name },
            onConfirm = { name ->
                playlistViewModel.addPlaylist(name)
            },
            onDismiss = {
                playlistViewModel.updateShowCreateDialog(false)
                playlistViewModel.updateNewPlaylistName("")
            }
        )
    }
}
