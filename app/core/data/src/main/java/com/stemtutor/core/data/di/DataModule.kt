package com.stemtutor.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.stemtutor.core.data.audio.AudioPlayer
import com.stemtutor.core.data.audio.AudioRepository
import com.stemtutor.core.data.audio.ExoAudioPlayer
import com.stemtutor.core.data.audio.FakeAudioRepository
import com.stemtutor.core.data.audio.ListenController
import com.stemtutor.core.data.audio.RemoteAudioRepository
import com.stemtutor.core.data.auth.AuthRepository
import com.stemtutor.core.data.auth.EncryptedSessionManager
import com.stemtutor.core.data.auth.FakeAuthRepository
import com.stemtutor.core.data.auth.SupabaseAuthRepository
import com.stemtutor.core.data.classroom.ClassroomRepository
import com.stemtutor.core.data.classroom.FakeClassroomRepository
import com.stemtutor.core.data.classroom.RemoteClassroomRepository
import com.stemtutor.core.data.course.CourseRepository
import com.stemtutor.core.data.course.FakeCourseRepository
import com.stemtutor.core.data.course.GenerationWatcher
import com.stemtutor.core.data.course.RemoteCourseRepository
import com.stemtutor.core.data.health.FakeHealthRepository
import com.stemtutor.core.data.health.HealthRepository
import com.stemtutor.core.data.health.RealHealthRepository
import com.stemtutor.core.data.learning.FakeLearningRepository
import com.stemtutor.core.data.learning.LearningRepository
import com.stemtutor.core.data.learning.RemoteLearningRepository
import com.stemtutor.core.data.profile.FakeProfileRepository
import com.stemtutor.core.data.profile.ProfileRepository
import com.stemtutor.core.data.profile.RemoteProfileRepository
import com.stemtutor.core.data.session.PendingJoinKey
import com.stemtutor.core.data.session.SessionStore
import com.stemtutor.core.network.ApiConfig
import com.stemtutor.core.network.LearningApi
import com.stemtutor.core.network.StemApi
import com.stemtutor.core.network.auth.AccessTokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/** Real or fake implementations, chosen by ApiConfig.useRealApi (debug builds default to fake). */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideAuthRepository(config: ApiConfig, dataStore: DataStore<Preferences>): AuthRepository =
        if (config.useRealApi) {
            SupabaseAuthRepository(config.supabaseUrl, config.supabaseAnonKey, EncryptedSessionManager(dataStore))
        } else {
            FakeAuthRepository(dataStore)
        }

    @Provides
    fun provideAccessTokenProvider(auth: AuthRepository): AccessTokenProvider = auth

    @Provides
    @Singleton
    fun provideProfileRepository(
        config: ApiConfig,
        api: Provider<StemApi>,
        auth: AuthRepository,
        dataStore: DataStore<Preferences>,
    ): ProfileRepository =
        if (config.useRealApi) RemoteProfileRepository(api.get()) else FakeProfileRepository(auth, dataStore)

    @Provides
    @Singleton
    fun provideClassroomRepository(
        config: ApiConfig,
        api: Provider<StemApi>,
        auth: AuthRepository,
        profiles: ProfileRepository,
    ): ClassroomRepository =
        if (config.useRealApi) RemoteClassroomRepository(api.get()) else FakeClassroomRepository(auth, profiles)

    @Provides
    @Singleton
    fun provideCourseRepository(config: ApiConfig, api: Provider<StemApi>, auth: AuthRepository): CourseRepository =
        if (config.useRealApi) RemoteCourseRepository(api.get()) else FakeCourseRepository(auth)

    @Provides
    @Singleton
    fun provideGenerationWatcher(courses: CourseRepository): GenerationWatcher = GenerationWatcher(courses)

    @Provides
    @Singleton
    fun provideHealthRepository(config: ApiConfig, api: Provider<StemApi>): HealthRepository =
        if (config.useRealApi) RealHealthRepository(api.get()) else FakeHealthRepository()

    @Provides
    @Singleton
    fun provideLearningRepository(config: ApiConfig, api: Provider<LearningApi>): LearningRepository =
        if (config.useRealApi) RemoteLearningRepository(api.get()) else FakeLearningRepository()

    @Provides
    @Singleton
    fun provideAudioRepository(config: ApiConfig, api: Provider<LearningApi>): AudioRepository =
        if (config.useRealApi) RemoteAudioRepository(api.get()) else FakeAudioRepository()

    @Provides
    @Singleton
    fun provideAudioPlayer(@ApplicationContext context: Context): AudioPlayer = ExoAudioPlayer(context)

    @Provides
    @Singleton
    fun provideListenController(audio: AudioRepository, player: AudioPlayer): ListenController =
        ListenController(audio, player)

    @Provides
    @Singleton
    fun provideSessionStore(dataStore: DataStore<Preferences>): SessionStore = SessionStore(dataStore)

    @Provides
    @Singleton
    fun providePendingJoinKey(): PendingJoinKey = PendingJoinKey()
}
