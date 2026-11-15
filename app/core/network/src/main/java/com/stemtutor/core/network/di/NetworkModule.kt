package com.stemtutor.core.network.di

import com.stemtutor.core.network.ApiConfig
import com.stemtutor.core.network.NetworkFactory
import com.stemtutor.core.network.FamilyApi
import com.stemtutor.core.network.LearningApi
import com.stemtutor.core.network.StemApi
import com.stemtutor.core.network.auth.AccessTokenProvider
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
    fun provideOkHttp(config: ApiConfig, tokens: AccessTokenProvider): OkHttpClient =
        NetworkFactory.okHttpClient(config.enableHttpLogging, tokens)

    @Provides
    @Singleton
    fun provideStemApi(config: ApiConfig, client: OkHttpClient): StemApi =
        NetworkFactory.stemApi(config.baseUrl, client)

    @Provides
    @Singleton
    fun provideLearningApi(config: ApiConfig, client: OkHttpClient): LearningApi =
        NetworkFactory.learningApi(config.baseUrl, client)

    @Provides
    @Singleton
    fun provideFamilyApi(config: ApiConfig, client: OkHttpClient): FamilyApi =
        NetworkFactory.familyApi(config.baseUrl, client)
}
