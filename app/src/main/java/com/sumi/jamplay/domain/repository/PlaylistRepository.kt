package com.sumi.jamplay.domain.repository

import com.sumi.jamplay.domain.model.Playlist
import com.sumi.jamplay.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {

    fun getAllPlaylists(favoritesId: Long): Flow<List<Playlist>>
    fun getPlaylistById(playlistId: Long): Flow<Playlist?>
    suspend fun addPlaylist(playlist: Playlist)
    suspend fun addTrackToPlaylist(playlistId: Long, track: Track)
    suspend fun deleteTrackFromPlaylist(playlistId: Long, track: Track)
    suspend fun deletePlaylist(playlistId: Long)
    fun getTracksOfPlaylist(playlistId: Long): Flow<List<Track>>
    suspend fun renamePlaylistWithTracks(
        oldId: Long,
        newName: String,
        tracks: List<Track>
    )
}