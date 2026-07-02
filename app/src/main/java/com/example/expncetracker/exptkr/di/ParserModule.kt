package com.example.expncetracker.exptkr.di

import com.example.expncetracker.exptkr.core.parser.BankParser
import com.example.expncetracker.exptkr.core.parser.ParserRegistry
import com.example.expncetracker.exptkr.core.parser.TransactionCategorizer
import com.example.expncetracker.exptkr.core.parser.TransactionCategorizerImpl
import com.example.expncetracker.exptkr.core.ml.HybridMlEngine
import com.example.expncetracker.exptkr.core.parser.bank.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ParserModule {
    
    @Provides
    @Singleton
    fun provideParsers(): List<BankParser> {
        return listOf(
            HdfcParser(),
            SbiParser(),
            IciciParser(),
            AxisParser(),
            KotakParser(),
            DopBankParser()
        )
    }

    @Provides
    @Singleton
    fun provideParserRegistry(parsers: List<@JvmSuppressWildcards BankParser>): ParserRegistry {
        return ParserRegistry(parsers)
    }

    @Provides
    @Singleton
    fun provideTransactionCategorizer(
        parserRegistry: ParserRegistry,
        mlEngine: HybridMlEngine
    ): TransactionCategorizer {
        return TransactionCategorizerImpl(parserRegistry, mlEngine)
    }
}
