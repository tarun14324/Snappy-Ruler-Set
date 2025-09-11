package com.tarun.snappyrulerset.di

import android.content.Context
import com.tarun.snappyrulerset.data.repository.DrawingRepositoryImpl
import com.tarun.snappyrulerset.domain.repository.DrawingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideDrawingRepositoryImpl(@ApplicationContext context: Context): DrawingRepository =
        DrawingRepositoryImpl(context = context)
}