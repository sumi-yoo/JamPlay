package com.sumi.jamplay.domain.model

data class Track(
    val id: Long,
    val name: String,
    val artistName: String,
    val albumName: String?,
    val artworkUrl: String?,
    val streamUrl: String
)
