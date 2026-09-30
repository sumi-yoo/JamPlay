package com.sumi.jamplay.data.repository

import com.sumi.jamplay.data.datasource.JamendoRemoteDataSource
import com.sumi.jamplay.data.mapper.toDomain
import com.sumi.jamplay.domain.model.TrackPage
import com.sumi.jamplay.domain.repository.TrackRepository
import javax.inject.Inject

class TrackRepositoryImpl @Inject constructor(
    private val remoteDataSource: JamendoRemoteDataSource
) : TrackRepository {
    override suspend fun searchTracks(query: String, offset: Int, limit: Int): TrackPage {
        val response = remoteDataSource.searchTracks(query, offset, limit)
        val tracks = response.results.map { it.toDomain() }
        return TrackPage(
            tracks = tracks,
            nextOffset = if (tracks.isEmpty()) null else offset + tracks.size
        )
    }
}