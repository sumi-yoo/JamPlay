package com.sumi.jamplay.domain.model

data class TrackPage(
    val tracks: List<Track>,
    val nextOffset: Int?
)
