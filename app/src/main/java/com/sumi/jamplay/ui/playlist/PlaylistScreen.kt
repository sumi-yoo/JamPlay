package com.sumi.jamplay.ui.playlist

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.sumi.jamplay.domain.policy.PlaylistNamePolicy
import com.sumi.jamplay.R
import com.sumi.jamplay.domain.model.Playlist

@Composable
fun PlaylistScreen(
    padding: PaddingValues,
    playlistViewModel: PlaylistViewModel,
    onPlaylistClick: (Long) -> Unit
) {
    val playlists by playlistViewModel.playlists.collectAsState()
    val isDeleteMode by playlistViewModel.deletePlayListMode.collectAsState()
    val favoritesName = stringResource(R.string.favorites_playlist_name)

    LaunchedEffect(Unit) {
        playlistViewModel.setFavoritesId(favoritesName)
    }

    Column(
        modifier = Modifier.run {
            fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.playlist_title),
                style = MaterialTheme.typography.titleLarge
            )

            if (isDeleteMode) {
                TextButton(onClick = {
                    val idsToDelete =
                        playlistViewModel.deletedPlaylists.filterValues { it }.keys
                    playlistViewModel.deletePlaylists(idsToDelete.toList())
                    playlistViewModel.clearSelectionPlaylists()
                    playlistViewModel.toggleDeletePlayListMode()
                }) {
                    Text(stringResource(R.string.complete))
                }
            } else {
                var expanded by rememberSaveable { mutableStateOf(false) }

                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        offset = DpOffset(x = (-5).dp, y = 0.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add), color = Color.White) },
                            onClick = {
                                expanded = false
                                playlistViewModel.updateShowCreateDialog(true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete), color = Color.White) },
                            onClick = {
                                expanded = false
                                playlistViewModel.clearSelectionPlaylists()
                                playlistViewModel.toggleDeletePlayListMode()
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_playlists),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                items(playlists) { playlist ->
                    val checked = playlistViewModel.deletedPlaylists[playlist.id] ?: false
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable(enabled = !isDeleteMode) {
                                if (!isDeleteMode) onPlaylistClick(playlist.id)
                            },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            contentColor = Color.White          // 내부 Text, Icon 색 기본값
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (playlist.tracks.isEmpty()) {
                                Icon(
                                    Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp)
                                )
                            } else {
                                AsyncImage(
                                    model = playlist.tracks.first().artworkUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }

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

                            if (isDeleteMode && playlist.id != favoritesName.hashCode().toLong()) {
                                Checkbox(
                                    modifier = Modifier
                                        .padding(5.dp)
                                        .size(20.dp),
                                    checked = checked,
                                    onCheckedChange = {
                                        playlistViewModel.deletedPlaylists[playlist.id] = it
                                    }
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null
                                )
                            }
                        }
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

@Composable
fun CreatePlaylistDialog(
    title: String,
    initialName: String = "",
    existingNames: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var showError by rememberSaveable { mutableStateOf(false) }
    var showEmpty by rememberSaveable { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF252525))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 제목
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )

                // 입력창
                TextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                        showEmpty = false
                    },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.playlist_name_label),
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF484747),
                        unfocusedContainerColor = Color(0xFF313131),
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // 오류 메시지
                if (showEmpty) {
                    Text(
                        text = stringResource(R.string.playlist_name_empty),
                        color = Color(0xFFFF6B6B),
                        fontSize = 13.sp
                    )
                }
                if (showError) {
                    Text(
                        text = if (initialName.isEmpty()) stringResource(R.string.playlist_name_exists) else  stringResource(R.string.playlist_name_same_as_before),
                        color = Color(0xFFFF6B6B),
                        fontSize = 13.sp
                    )
                }

                // 버튼들 (취소 / 확인)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = Color.White.copy(alpha = 0.7f))
                    }

                    TextButton(
                        onClick = {
                            when (PlaylistNamePolicy.validate(name, existingNames)) {
                                PlaylistNamePolicy.Error.EMPTY -> {
                                    showEmpty = true
                                    showError = false
                                }
                                PlaylistNamePolicy.Error.DUPLICATE -> {
                                    showEmpty = false
                                    showError = true
                                }
                                null -> {
                                    onConfirm(name.trim())
                                    onDismiss()
                                }
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.confirm),
                            color = Color(0xFF6B4EFF),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}