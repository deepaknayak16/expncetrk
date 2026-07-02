package com.example.expncetracker.exptkr.core.ml

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.example.expncetracker.exptkr.core.common.LAST_RULE_SEED_VERSION
import com.example.expncetracker.exptkr.core.common.Logger
import com.example.expncetracker.exptkr.core.common.dataStore
import com.example.expncetracker.exptkr.domain.model.ClassificationRule
import com.example.expncetracker.exptkr.domain.model.MatchType
import com.example.expncetracker.exptkr.domain.repository.RuleRepository
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ruleRepository: RuleRepository,
    private val transactionRepository: TransactionRepository,
    private val mlEngine: HybridMlEngine
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun seedIfNeeded() {
        try {
            // FIX 2: Version-based seeding
            val metadataString = context.assets.open("rules/metadata.json").bufferedReader().use { it.readText() }
            val metadata = json.decodeFromString<Metadata>(metadataString)
            
            val prefs = context.dataStore.data.first()
            val lastVersion = prefs[LAST_RULE_SEED_VERSION] ?: 0

            if (metadata.version_code <= lastVersion) {
                Logger.d("RuleSeeder", "Rules are up to date (v${metadata.version_code})")
                return
            }

            Logger.d("RuleSeeder", "Updating rules from v$lastVersion to v${metadata.version_code}...")
            
            val jsonString = context.assets.open("rules/entities.json").bufferedReader().use { it.readText() }
            val entities = json.decodeFromString<List<RawEntity>>(jsonString)

            val rulesToSeed = entities.flatMap { entity ->
                entity.aliases.flatMap { alias ->
                    // FIX 3: Alias Expansion
                    val keywords = mutableListOf<String>()
                    val cleanAlias = alias.replace("_", " ")
                    keywords.add(cleanAlias) // "HDFC BANK"
                    keywords.add(alias.replace("_", ""))  // "HDFCBANK"
                    
                    // Add "Brand Level" keyword (first word) for broader matching (e.g., "AIRTEL", "JIO")
                    val brandWord = cleanAlias.split(" ").firstOrNull()
                    if (brandWord != null && brandWord.length >= 3) {
                        keywords.add(brandWord)
                    }
                    
                    keywords.distinct().map { kw ->
                        ClassificationRule(
                            keyword = kw,
                            category = entity.category,
                            matchType = MatchType.CONTAINS,
                            priority = 90,
                            transactionType = when (entity.type) {
                                "INCOME" -> "CREDIT"
                                "BANK" -> null
                                else -> "DEBIT"
                            },
                            isSystemRule = true
                        )
                    }
                }
            }

            if (rulesToSeed.isNotEmpty()) {
                ruleRepository.deleteSystemRules()
                ruleRepository.insertRules(rulesToSeed)
                
                context.dataStore.edit { it[LAST_RULE_SEED_VERSION] = metadata.version_code }
                Logger.d("RuleSeeder", "Successfully seeded ${rulesToSeed.size} rules.")
                
                // FIX 4: Retroactive Re-categorization
                runRetroactiveCategorization()
            }
        } catch (e: Exception) {
            Logger.e("RuleSeeder", "Failed to seed rules", e)
        }
    }

    private suspend fun runRetroactiveCategorization() {
        Logger.d("RuleSeeder", "Running retroactive re-categorization...")
        val transactions = transactionRepository.getRecentTransactions(500).first()
        val blindSpots = transactions.filter { 
            (it.categoryName == "Others" || it.confidenceScore < 0.5f) && 
            !it.isCategoryManuallyCorrected
        }
        
        if (blindSpots.isEmpty()) return
        
        Logger.d("RuleSeeder", "Found ${blindSpots.size} blind spots to re-categorize")
        
        blindSpots.forEach { tx ->
            val result = mlEngine.infer(
                merchantName = tx.merchant,
                amount = tx.amount,
                type = tx.type,
                timestamp = tx.timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                smsBody = tx.rawSmsBody ?: ""
            )
            
            if (result.category != tx.categoryName && result.confidenceScore > 0.8f) {
                transactionRepository.updateTransactionCategory(tx.id, result.category, result.confidenceScore)
                Logger.d("RuleSeeder", "Re-categorized ${tx.merchant}: ${tx.categoryName} -> ${result.category}")
            }
        }
    }

    @Serializable
    private data class Metadata(val version_code: Int)

    @Serializable
    private data class RawEntity(
        val category: String,
        val type: String,
        val aliases: List<String>
    )
}
