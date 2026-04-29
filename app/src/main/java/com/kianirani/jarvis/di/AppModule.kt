package com.kianirani.jarvis.di

import com.kianirani.jarvis.data.repository.BrainRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideBrainRepository(): BrainRepository = BrainRepository()
}
