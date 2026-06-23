package com.example.expncetracker.exptkr.domain.repository

import com.example.expncetracker.exptkr.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<CategoryEntity>>
    suspend fun insertCategory(category: CategoryEntity)
    suspend fun deleteCategory(category: CategoryEntity)
}
