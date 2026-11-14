package com.stemtutor.app.di

import com.stemtutor.app.BuildConfig
import com.stemtutor.core.network.ApiConfig
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
    fun provideApiConfig(): ApiConfig = ApiConfig(
        baseUrl = BuildConfig.API_BASE_URL,
        useRealApi = BuildConfig.USE_REAL_API,
        enableHttpLogging = BuildConfig.DEBUG,
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
        joinLinkHost = BuildConfig.JOIN_LINK_HOST,
    )
}
