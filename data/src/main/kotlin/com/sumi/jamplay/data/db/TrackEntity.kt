package com.sumi.jamplay.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val artistName: String,
    val albumName: String?,
    val artworkUrl: String?,
    val streamUrl: String
)