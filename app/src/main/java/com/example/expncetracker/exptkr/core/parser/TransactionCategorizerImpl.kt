package com.example.expncetracker.exptkr.core.parser

import com.example.expncetracker.exptkr.core.common.toEpochMilli
import com.example.expncetracker.exptkr.core.ml.HybridMlEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionCategorizerImpl @Inject constructor(
    private val parserRegistry: ParserRegistry,
    private val mlEngine: HybridMlEngine
) : TransactionCategorizer {

    override suspend fun findMatch(smsBody: String): CategorizationResult {
        // We use a generic sender identifier as we only have the body
        val parsedSms = parserRegistry.parseSms("GENERIC", smsBody, System.currentTimeMillis())
            ?: return CategorizationResult("others", "Unknown")

        val mlResult = mlEngine.infer(
            merchantName = parsedSms.merchant,
            amount = parsedSms.amount,
            type = parsedSms.type,
            timestamp = parsedSms.timestamp.toEpochMilli(),
            smsBody = smsBody
        )

        return CategorizationResult(
            categorySlug = mlResult.category,
            entityName = parsedSms.merchant
        )
    }
}
