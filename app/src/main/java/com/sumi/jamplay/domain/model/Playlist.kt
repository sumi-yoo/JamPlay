package com.sumi.jamplay.domain.model

data class Playlist(
    val id: Long,
    val name: String,
    val tracks: List<Track> = mutableListOf()
)
