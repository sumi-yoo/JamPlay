package com.sumi.jamplay.di

import com.sumi.jamplay.data.repository.PlaylistRepositoryImpl
import com.sumi.jamplay.data.repository.SearchHistoryRepositoryImpl
import com.sumi.jamplay.data.repository.TrackRepositoryImpl
import com.sumi.jamplay.domain.repository.PlaylistRepository
import com.sumi.jamplay.domain.repository.SearchHistoryRepository
import com.sumi.jamplay.domain.repository.TrackRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindSearchHistoryRepository(
        impl: SearchHistoryRepositoryImpl
    ): SearchHistoryRepository

    @Binds
    abstract fun bindTrackRepository(
        impl: TrackRepositoryImpl
    ): TrackRepository

    @Binds
    abstract fun bindPlaylistRepository(
        impl: PlaylistRepositoryImpl
    ): PlaylistRepository
}
