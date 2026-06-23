package com.example.expncetracker.exptkr.di

import com.example.expncetracker.exptkr.data.repository.AccountRepositoryImpl
import com.example.expncetracker.exptkr.data.repository.BudgetRepositoryImpl
import com.example.expncetracker.exptkr.data.repository.CategoryRepositoryImpl
import com.example.expncetracker.exptkr.data.repository.GoalRepositoryImpl
import com.example.expncetracker.exptkr.data.repository.MerchantMappingRepositoryImpl
import com.example.expncetracker.exptkr.data.repository.TransactionRepositoryImpl
import com.example.expncetracker.exptkr.domain.repository.AccountRepository
import com.example.expncetracker.exptkr.domain.repository.BudgetRepository
import com.example.expncetracker.exptkr.domain.repository.CategoryRepository
import com.example.expncetracker.exptkr.domain.repository.GoalRepository
import com.example.expncetracker.exptkr.domain.repository.MerchantMappingRepository
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
    @Binds @Singleton abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository
    @Binds @Singleton abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository
    @Binds @Singleton abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository
    @Binds @Singleton abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository
    @Binds @Singleton abstract fun bindMerchantMappingRepository(impl: MerchantMappingRepositoryImpl): MerchantMappingRepository
}
