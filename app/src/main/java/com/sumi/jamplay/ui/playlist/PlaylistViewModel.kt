package com.sumi.jamplay.ui.playlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumi.jamplay.domain.model.Playlist
import com.sumi.jamplay.domain.model.Track
import com.sumi.jamplay.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val repository: PlaylistRepository
) : ViewModel() {

    private val _favoritesId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val playlists: StateFlow<List<Playlist>> = _favoritesId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getAllPlaylists(id)
            } else {
                flowOf(emptyList()) // null이면 빈 리스트
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _playlistId = MutableStateFlow<Long?>(null)
    val playlistId = _playlistId.asStateFlow()

    private val _deletePlayListMode = MutableStateFlow(false)
    val deletePlayListMode: StateFlow<Boolean> = _deletePlayListMode

    val selectedPlaylist: StateFlow<Playlist?> = _playlistId
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { id ->
            repository.getPlaylistById(id)
        }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val tracks: StateFlow<List<Track>> = _playlistId
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { id ->
            repository.getTracksOfPlaylist(id)
        }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    var showCreateDialog by mutableStateOf(false)
        private set

    var newPlaylistName by mutableStateOf("")
        private set

    val selectedPlaylists = mutableStateMapOf<Long, Boolean>()

    val deletedPlaylists = mutableStateMapOf<Long, Boolean>()

    // 선택 모드 상태
    var deleteTrackMode by mutableStateOf(false)
        private set

    val deletedTracks = mutableStateMapOf<Long, Boolean>()

    var acceptsClicks by mutableStateOf(true)
        private set

    fun isTrackInPlaylist(playlistId: Long, trackId: Long): Boolean {
        return playlists.value.firstOrNull { it.id == playlistId }?.tracks?.any { it.id == trackId } == true
    }

    fun addTrackToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track)
        }
    }

    fun addPlaylist(name: String) {
        val trimmedName = name.trim()
        val playlistId = trimmedName.hashCode().toLong()
        viewModelScope.launch {
            repository.addPlaylist(Playlist(id = playlistId, name = trimmedName))
        }
    }

    fun deleteTrackFromPlaylist(playlistId: Long? = _playlistId.value, track: Track) {
        playlistId?.let {
            viewModelScope.launch {
                repository.deleteTrackFromPlaylist(it, track)
            }
        }
    }

    fun getTracksOfPlaylist(playlistId: Long): Flow<List<Track>> {
        return repository.getTracksOfPlaylist(playlistId)
    }

    fun toggleDeletePlayListMode() {
        _deletePlayListMode.value = !_deletePlayListMode.value
    }

    fun deletePlaylists(ids: List<Long>) {
        viewModelScope.launch {
            ids.forEach { id ->
                repository.deletePlaylist(id)
            }
        }
    }

    fun setFavoritesId(favoritesName: String) {
        _favoritesId.value = favoritesName.hashCode().toLong()
    }

    fun updateShowCreateDialog(show: Boolean) {
        showCreateDialog = show
    }

    fun updateNewPlaylistName(name: String) {
        newPlaylistName = name
    }

    fun clearSelectedPlaylists() {
        selectedPlaylists.clear()
    }

    fun clearSelectionPlaylists() {
        deletedPlaylists.clear()
    }

    fun updateDeleteTrackMode(enabled: Boolean) {
        deleteTrackMode = enabled
    }

    fun clearSelectionTracks() {
        deletedTracks.clear()
    }

    fun setPlaylistId(id: Long) {
        _playlistId.value = id
    }

    fun renamePlaylist(newName: String, tracks: List<Track>) {
        _playlistId.value?.let {
            viewModelScope.launch {
                repository.renamePlaylistWithTracks(it, newName, tracks)
                val newId = newName.trim().hashCode().toLong()
                setPlaylistId(newId)
            }
        }
    }

    fun enableClicks() { acceptsClicks = true }

    fun disableClicks() { acceptsClicks = false }
}