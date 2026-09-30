package com.sumi.jamplay.domain.repository

import com.sumi.jamplay.domain.model.TrackPage

interface TrackRepository {
    suspend fun searchTracks(query: String, offset: Int, limit: Int): TrackPage
}