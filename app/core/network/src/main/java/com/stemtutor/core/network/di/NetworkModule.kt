package com.stemtutor.core.network.di

import com.stemtutor.core.network.ApiConfig
import com.stemtutor.core.network.NetworkFactory
import com.stemtutor.core.network.StemApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttp(config: ApiConfig): OkHttpClient =
        NetworkFactory.okHttpClient(config.enableHttpLogging)

    @Provides
    @Singleton
    fun provideStemApi(config: ApiConfig, client: OkHttpClient): StemApi =
        NetworkFactory.stemApi(config.baseUrl, client)
}
