package com.sumi.jamplay.di

import com.sumi.jamplay.domain.playback.PlaybackController
import com.sumi.jamplay.service.ServicePlaybackController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class PlaybackModule {
    @Binds
    @ViewModelScoped
    abstract fun bindPlaybackController(impl: ServicePlaybackController): PlaybackController
}
