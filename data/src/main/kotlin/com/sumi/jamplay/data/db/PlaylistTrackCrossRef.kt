package com.sumi.jamplay.data.db

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "playlist_track_cross_ref",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE // 플레이리스트 삭제 시 연결관계도 함께 삭제
        ),
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE // 트랙 삭제 시 관계도 함께 삭제
        )
    ]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: Long
)