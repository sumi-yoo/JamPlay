package com.sumi.jamplay.data.repository

import com.sumi.jamplay.data.db.PlaylistDao
import com.sumi.jamplay.data.db.PlaylistEntity
import com.sumi.jamplay.data.db.TrackEntity
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
            list.map { pl ->
                Playlist(
                    id = pl.playlist.id,
                    name = pl.playlist.name,
                    tracks = pl.tracks.map { t ->
                        Track(
                            id = t.id,
                            name = t.name,
                            artistName = t.artistName,
                            albumName = t.albumName,
                            artworkUrl = t.artworkUrl,
                            streamUrl = t.streamUrl
                        )
                    }
                )
            }
        }

    override fun getPlaylistById(playlistId: Long): Flow<Playlist?> {
        return dao.getPlaylistById(playlistId)
            .map { pl ->
                pl?.let {
                    Playlist(
                        id = it.playlist.id,
                        name = it.playlist.name,
                        tracks = it.tracks.map { t ->
                            Track(
                                id = t.id,
                                name = t.name,
                                artistName = t.artistName,
                                albumName = t.albumName,
                                artworkUrl = t.artworkUrl,
                                streamUrl = t.streamUrl
                            )
                        }
                    )
                }
            }
    }

    // 플레이리스트 추가
    override suspend fun addPlaylist(playlist: Playlist) {
        dao.insertPlaylist(
            PlaylistEntity(
                id = playlist.id,
                name = playlist.name
            )
        )
    }

    // 트랙 추가
    override suspend fun addTrackToPlaylist(playlistId: Long, track: Track) {
        val trackEntity = TrackEntity(
            id = track.id,
            name = track.name,
            artistName = track.artistName,
            albumName = track.albumName,
            artworkUrl = track.artworkUrl,
            streamUrl = track.streamUrl
        )
        dao.insertTrackToPlaylist(trackEntity, playlistId)
    }

    override suspend fun deleteTrackFromPlaylist(playlistId: Long, track: Track) {
        dao.deleteTrackFromPlaylist(playlistId, track.id)
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylistSafe(playlistId)
    }

    override fun getTracksOfPlaylist(playlistId: Long): Flow<List<Track>> {
        return dao.getTracksOfPlaylist(playlistId).map { entities ->
            entities.map { track ->
                Track(
                    id = track.id,
                    name = track.name,
                    artistName = track.artistName,
                    albumName = track.albumName,
                    artworkUrl = track.artworkUrl,
                    streamUrl = track.streamUrl
                )
            }
        }
    }

    override suspend fun renamePlaylistWithTracks(
        oldId: Long,
        newName: String,
        tracks: List<Track>
    ) {
        val newId = newName.trim().hashCode().toLong()
        val newEntity = PlaylistEntity(id = newId, name = newName.trim())

        val trackEntities = tracks.map {
            TrackEntity(
                id = it.id,
                name = it.name,
                artistName = it.artistName,
                albumName = it.albumName,
                artworkUrl = it.artworkUrl,
                streamUrl = it.streamUrl
            )
        }

        dao.renamePlaylistWithTracks(oldId, newEntity, trackEntities)
    }
}