package com.example.expncetracker.exptkr.di

import com.example.expncetracker.exptkr.data.repository.TransactionRepositoryImpl
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindRepository(impl: TransactionRepositoryImpl): TransactionRepository
}
