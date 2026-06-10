package com.example.expncetracker.exptkr.di

import com.example.expncetracker.exptkr.core.parser.ParserRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ParserModule {
    @Provides @Singleton fun provideParserRegistry(): ParserRegistry = ParserRegistry()
}
