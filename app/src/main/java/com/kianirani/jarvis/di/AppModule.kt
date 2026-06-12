package com.kianirani.jarvis.di

import android.content.Context
import com.kianirani.jarvis.brain.discovery.BrainHandshake
import com.kianirani.jarvis.brain.discovery.BrainSelectionStore
import com.kianirani.jarvis.brain.discovery.PrefsBrainSelectionStore
import com.kianirani.jarvis.brain.discovery.DiscoveryScanner
import com.kianirani.jarvis.brain.discovery.HttpBrainHandshake
import com.kianirani.jarvis.brain.discovery.NsdDiscovery
import com.kianirani.jarvis.brain.discovery.NsdDiscoveryScanner
import com.kianirani.jarvis.brain.score.AndroidDeviceMetricsProvider
import com.kianirani.jarvis.brain.score.LocalDeviceMetricsProvider
import com.kianirani.jarvis.data.ai.AiProviderStore
import com.kianirani.jarvis.data.ai.PrefsAiProviderStore
import com.kianirani.jarvis.data.repository.BrainRepository
import com.kianirani.jarvis.voice.AndroidVoiceController
import com.kianirani.jarvis.voice.VoiceController
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
    fun provideBrainRepository(store: BrainSelectionStore): BrainRepository = BrainRepository(store)

    @Provides @Singleton
    fun provideLocalDeviceMetrics(@ApplicationContext ctx: Context): LocalDeviceMetricsProvider =
        AndroidDeviceMetricsProvider(ctx)

    @Provides @Singleton
    fun provideDiscoveryScanner(@ApplicationContext ctx: Context): DiscoveryScanner =
        NsdDiscoveryScanner(NsdDiscovery(ctx))

    @Provides @Singleton
    fun provideBrainHandshake(): BrainHandshake = HttpBrainHandshake()

    @Provides @Singleton
    fun provideBrainSelectionStore(@ApplicationContext ctx: Context): BrainSelectionStore =
        PrefsBrainSelectionStore(ctx)

    @Provides @Singleton
    fun provideAiProviderStore(@ApplicationContext ctx: Context): AiProviderStore =
        PrefsAiProviderStore(ctx)

    @Provides @Singleton
    fun provideVoiceController(
        @ApplicationContext ctx: Context,
        settings: com.kianirani.jarvis.data.settings.VisionSettings,
    ): VoiceController = AndroidVoiceController(ctx, settings)
}
