package com.sumi.jamplay.data.mapper

import com.sumi.jamplay.data.db.PlaylistEntity
import com.sumi.jamplay.data.db.PlaylistWithTracks
import com.sumi.jamplay.domain.model.Playlist

fun PlaylistWithTracks.toDomain(): Playlist = Playlist(
    id = playlist.id,
    name = playlist.name,
    tracks = tracks.map { it.toDomain() }
)

fun Playlist.toEntity(): PlaylistEntity = PlaylistEntity(
    id = id,
    name = name
)
