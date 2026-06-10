package com.example.expncetracker.exptkr.di

import android.content.Context
import com.example.expncetracker.exptkr.core.sync.GoogleDriveSyncManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideGoogleDriveSyncManager(
        @ApplicationContext context: Context
    ): GoogleDriveSyncManager {
        return GoogleDriveSyncManager(context)
    }
}
