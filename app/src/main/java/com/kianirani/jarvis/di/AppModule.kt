package com.kianirani.jarvis.di

import android.content.Context
import com.kianirani.jarvis.brain.score.AndroidDeviceMetricsProvider
import com.kianirani.jarvis.brain.score.LocalDeviceMetricsProvider
import com.kianirani.jarvis.data.repository.BrainRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideBrainRepository(): BrainRepository = BrainRepository()

    @Provides @Singleton
    fun provideLocalDeviceMetrics(@ApplicationContext ctx: Context): LocalDeviceMetricsProvider =
        AndroidDeviceMetricsProvider(ctx)
}
