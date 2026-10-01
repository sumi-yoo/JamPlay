package com.sumi.jamplay.data.model

import com.google.gson.annotations.SerializedName

data class JamendoTrackResponse(
    val headers: Map<String, Any>,
    @SerializedName("results") val results: List<JamendoTrack>
)

data class JamendoTrack(
    val id: Long,
    val name: String,
    @SerializedName("artist_name") val artistName: String,
    @SerializedName("album_name") val albumName: String?,
    @SerializedName("audio") val audioUrl: String,
    @SerializedName("album_image") val artworkUrl: String?,
    @SerializedName("duration") val duration: Int?,          // 트랙 길이 (초)
    @SerializedName("license_ccurl") val licenseUrl: String?, // 라이선스 URL
    @SerializedName("releasedate") val releaseDate: String?  // 발매일
)