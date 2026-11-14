package com.stemtutor.core.data.di

import com.stemtutor.core.data.health.FakeHealthRepository
import com.stemtutor.core.data.health.HealthRepository
import com.stemtutor.core.data.health.RealHealthRepository
import com.stemtutor.core.network.ApiConfig
import com.stemtutor.core.network.StemApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideHealthRepository(config: ApiConfig, api: Provider<StemApi>): HealthRepository =
        if (config.useRealApi) RealHealthRepository(api.get()) else FakeHealthRepository()
}
