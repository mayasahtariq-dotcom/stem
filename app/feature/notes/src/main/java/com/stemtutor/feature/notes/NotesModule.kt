package com.stemtutor.feature.notes

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NotesModule {
    @Binds
    abstract fun bindPageProcessor(impl: AndroidPageProcessor): PageProcessor
}
