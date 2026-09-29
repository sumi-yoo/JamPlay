package com.sumi.jamplay.data.mapper

import com.sumi.jamplay.data.db.TrackEntity
import com.sumi.jamplay.data.model.JamendoTrack
import com.sumi.jamplay.domain.model.Track

fun JamendoTrack.toDomain(): Track = Track(
    id = id,
    name = name,
    artistName = artistName,
    albumName = albumName,
    artworkUrl = artworkUrl,
    streamUrl = audioUrl
)

fun TrackEntity.toDomain(): Track = Track(
    id = id,
    name = name,
    artistName = artistName,
    albumName = albumName,
    artworkUrl = artworkUrl,
    streamUrl = streamUrl
)

fun Track.toEntity(): TrackEntity = TrackEntity(
    id = id,
    name = name,
    artistName = artistName,
    albumName = albumName,
    artworkUrl = artworkUrl,
    streamUrl = streamUrl
)
