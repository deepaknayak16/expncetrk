package com.example.expncetracker.exptkr.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier annotation class IoDispatcher
@Qualifier annotation class MainDispatcher
@Qualifier annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @IoDispatcher @Provides fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    @MainDispatcher @Provides fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
    
    @ApplicationScope
    @Provides
    @Singleton
    fun provideApplicationScope(@IoDispatcher ioDispatcher: CoroutineDispatcher): CoroutineScope = 
        CoroutineScope(SupervisorJob() + ioDispatcher)
}
