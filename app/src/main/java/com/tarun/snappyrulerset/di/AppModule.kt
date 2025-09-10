package com.tarun.snappyrulerset.di

import com.tarun.snappyrulerset.data.repository.DrawingRepositoryImpl
import com.tarun.snappyrulerset.domain.repository.DrawingRepository
import com.tarun.snappyrulerset.domain.snap.SnapEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSnapEngine(): SnapEngine = SnapEngine()

    @Provides
    fun provideDrawingRepositoryImpl(): DrawingRepository = DrawingRepositoryImpl()
}