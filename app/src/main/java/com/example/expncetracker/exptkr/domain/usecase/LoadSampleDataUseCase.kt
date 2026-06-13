package com.example.expncetracker.exptkr.domain.usecase

import android.content.Context
import com.example.expncetracker.exptkr.data.model.SampleDataDto
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class LoadSampleDataUseCase @Inject constructor(
    private val repository: TransactionRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun execute() {
        repository.clearAllTransactions()

        val jsonString = context.assets.open("sample_transactions.json").bufferedReader().use {
            it.readText()
        }

        val sampleData = Json.decodeFromString<SampleDataDto>(jsonString)
        
        val now = System.currentTimeMillis()
        val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000
        val totalItems = sampleData.transactions.size.toDouble()

        val samples = sampleData.transactions.mapIndexed { index, item ->
            val fraction = index / totalItems
            val timestamp = now - (fraction * thirtyDaysMillis).toLong()

            Transaction(
                amount = item.amount,
                type = when {
                    item.type == "INCOME" || item.category == "INCOME_GEM" || item.category == "INCOME_SALARY" -> TransactionType.CREDIT
                    else -> TransactionType.DEBIT
                },
                categoryName = mapCategory(item.category).name,
                merchant = item.description,
                bankName = item.bankName,
                timestamp = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()
                )
            )
        }

        samples.chunked(500).forEach { chunk ->
            repository.insertTransactions(chunk)
        }
    }

    private fun mapCategory(categoryStr: String): Category {
        return when (categoryStr.uppercase()) {
            "FOOD_DINING" -> Category.FOOD
            "CABS_TAXI" -> Category.CABS
            "RENT_HOUSING" -> Category.RENT
            "BILLS_UTILITIES", "CAR_RENTAL_FUEL" -> Category.BILLS
            "SHOPPING" -> Category.SHOPPING
            "INCOME_SALARY" -> Category.SALARY
            "INVESTMENT" -> Category.INVESTMENTS
            "HOTEL_TRAVEL" -> Category.TRAVEL
            else -> Category.OTHERS
        }
    }
}
