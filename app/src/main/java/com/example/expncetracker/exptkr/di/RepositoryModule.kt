package com.example.expncetracker.exptkr.di

import com.example.expncetracker.exptkr.data.repository.*
import com.example.expncetracker.exptkr.domain.repository.*
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
    @Binds @Singleton abstract fun bindRuleRepository(impl: RuleRepositoryImpl): RuleRepository
    @Binds @Singleton abstract fun bindEntityRepository(impl: EntityRepositoryImpl): EntityRepository
}
