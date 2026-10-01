package com.sumi.jamplay.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumi.jamplay.domain.model.Playlist
import com.sumi.jamplay.domain.model.Track
import com.sumi.jamplay.domain.usecase.CreatePlaylistUseCase
import com.sumi.jamplay.domain.usecase.SavePlaylistSelectionUseCase
import com.sumi.jamplay.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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
    private val repository: PlaylistRepository,
    private val createPlaylist: CreatePlaylistUseCase,
    private val saveSelection: SavePlaylistSelectionUseCase
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

    private val _selectedPlaylists = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val selectedPlaylists = _selectedPlaylists.asStateFlow()
    private var selectionTrackId: Long? = null

    private val _deletedPlaylists = MutableStateFlow<Set<Long>>(emptySet())
    val deletedPlaylists = _deletedPlaylists.asStateFlow()

    private val _deleteTrackMode = MutableStateFlow(false)
    val deleteTrackMode = _deleteTrackMode.asStateFlow()

    private val _deletedTracks = MutableStateFlow<Set<Long>>(emptySet())
    val deletedTracks = _deletedTracks.asStateFlow()

    fun initializeSelection(track: Track, playlists: List<Playlist>) {
        if (selectionTrackId != track.id) {
            selectionTrackId = track.id
            _selectedPlaylists.value = emptyMap()
        }
        _selectedPlaylists.update { selected ->
            playlists.associate { playlist ->
                playlist.id to (selected[playlist.id] ?: playlist.tracks.any { it.id == track.id })
            }
        }
    }

    fun setPlaylistSelected(id: Long, selected: Boolean) {
        _selectedPlaylists.update { it + (id to selected) }
    }

    fun setPlaylistDeleted(id: Long, selected: Boolean) {
        if (id == _favoritesId.value) return
        _deletedPlaylists.update { if (selected) it + id else it - id }
    }

    fun setTrackDeleted(id: Long, selected: Boolean) {
        _deletedTracks.update { if (selected) it + id else it - id }
    }

    fun deleteSelectedPlaylists() {
        deletePlaylists(_deletedPlaylists.value.toList())
        clearSelectionPlaylists()
        _deletePlayListMode.value = false
    }

    fun deleteSelectedTracks() {
        val id = _playlistId.value ?: return
        val selected = tracks.value.filter { it.id in _deletedTracks.value }
        viewModelScope.launch {
            selected.forEach { repository.deleteTrackFromPlaylist(id, it) }
        }
        clearSelectionTracks()
        updateDeleteTrackMode(false)
    }

    fun savePlaylistSelection(track: Track) {
        val selection = _selectedPlaylists.value.toMap()
        viewModelScope.launch {
            saveSelection(track, selection)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track)
        }
    }

    fun addPlaylist(name: String) {
        viewModelScope.launch {
            createPlaylist(name)
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

    fun clearSelectedPlaylists() {
        selectionTrackId = null
        _selectedPlaylists.value = emptyMap()
    }

    fun clearSelectionPlaylists() {
        _deletedPlaylists.value = emptySet()
    }

    fun updateDeleteTrackMode(enabled: Boolean) {
        _deleteTrackMode.value = enabled
    }

    fun clearSelectionTracks() {
        _deletedTracks.value = emptySet()
    }

    fun setPlaylistId(id: Long) {
        _playlistId.value = id
    }

    fun renamePlaylist(newName: String, playlistId: Long? = _playlistId.value) {
        playlistId?.let {
            viewModelScope.launch {
                repository.renamePlaylist(it, newName)
            }
        }
    }

}
