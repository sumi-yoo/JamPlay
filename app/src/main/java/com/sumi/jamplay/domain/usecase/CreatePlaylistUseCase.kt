package com.sumi.jamplay.domain.usecase

import com.sumi.jamplay.domain.model.Playlist
import com.sumi.jamplay.domain.repository.PlaylistRepository
import java.util.UUID
import javax.inject.Inject

class CreatePlaylistUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    suspend operator fun invoke(name: String) {
        repository.addPlaylist(
            Playlist(id = UUID.randomUUID().mostSignificantBits, name = name)
        )
    }
}
