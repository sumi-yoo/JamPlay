package com.sumi.jamplay.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.sumi.jamplay.domain.model.Track
import com.sumi.jamplay.data.datasource.JamendoRemoteDataSource
import com.sumi.jamplay.data.paging.JamendoPagingSource
import com.sumi.jamplay.domain.repository.TrackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TrackRepositoryImpl @Inject constructor(
    private val remoteDataSource: JamendoRemoteDataSource
) : TrackRepository {

    override fun searchTracks(query: String): Flow<PagingData<Track>> {
        return Pager(
            config = PagingConfig(pageSize = 30, prefetchDistance = 10, initialLoadSize = 60),
            pagingSourceFactory = { JamendoPagingSource(remoteDataSource, query) }
        ).flow
    }
}