package com.sumi.jamplay.domain.usecase

import com.sumi.jamplay.domain.model.Track
import com.sumi.jamplay.domain.repository.PlaylistRepository
import javax.inject.Inject

class SavePlaylistSelectionUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    suspend operator fun invoke(track: Track, selection: Map<Long, Boolean>) {
        selection.forEach { (playlistId, selected) ->
            if (selected) {
                repository.addTrackToPlaylist(playlistId, track)
            } else {
                repository.deleteTrackFromPlaylist(playlistId, track)
            }
        }
    }
}
