package com.example.expncetracker.exptkr.data.repository

import android.content.Context
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.repository.EntityRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntityRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : EntityRepository {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    override val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    init {
        loadEntities()
    }

    private fun loadEntities() {
        try {
            val jsonString = context.assets.open("rules/entities.json").bufferedReader().use { it.readText() }
            val json = Json { ignoreUnknownKeys = true }
            val rawEntities = json.decodeFromString<List<RawEntity>>(jsonString)
            
            _categories.value = rawEntities.map { 
                Category(
                    id = it.category, // FIX 1: Use category slug as ID for Single Source of Truth
                    name = it.name,
                    type = it.type,
                    icon = it.logo.removeSuffix(".png"),
                    color = "#4CAF50"
                )
            }
        } catch (e: Exception) {
            // Log error
        }
    }

    @kotlinx.serialization.Serializable
    private data class RawEntity(
        val id: String,
        val name: String,
        val category: String,
        val type: String,
        val logo: String
    )
}
