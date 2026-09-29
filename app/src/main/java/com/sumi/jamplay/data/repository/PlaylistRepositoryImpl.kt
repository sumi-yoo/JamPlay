package com.sumi.jamplay.data.repository

import com.sumi.jamplay.data.db.PlaylistDao
import com.sumi.jamplay.data.mapper.toDomain
import com.sumi.jamplay.data.mapper.toEntity
import com.sumi.jamplay.domain.model.Playlist
import com.sumi.jamplay.domain.model.Track
import com.sumi.jamplay.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlaylistRepositoryImpl @Inject constructor(
    private val dao: PlaylistDao
) : PlaylistRepository {

    // 플레이리스트 조회
    override fun getAllPlaylists(favoritesId: Long): Flow<List<Playlist>> =
        dao.getAllPlaylists(favoritesId).map { list ->
            list.map { it.toDomain() }
        }

    override fun getPlaylistById(playlistId: Long): Flow<Playlist?> {
        return dao.getPlaylistById(playlistId).map { it?.toDomain() }
    }

    // 플레이리스트 추가
    override suspend fun addPlaylist(playlist: Playlist) {
        dao.insertPlaylistIfNameAvailable(playlist.toEntity())
    }

    // 트랙 추가
    override suspend fun addTrackToPlaylist(playlistId: Long, track: Track) {
        dao.insertTrackToPlaylist(track.toEntity(), playlistId)
    }

    override suspend fun deleteTrackFromPlaylist(playlistId: Long, track: Track) {
        dao.deleteTrackFromPlaylist(playlistId, track.id)
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylistSafe(playlistId)
    }

    override fun getTracksOfPlaylist(playlistId: Long): Flow<List<Track>> {
        return dao.getTracksOfPlaylist(playlistId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun renamePlaylist(playlistId: Long, newName: String) {
        dao.renamePlaylistIfNameAvailable(playlistId, newName)
    }
}
