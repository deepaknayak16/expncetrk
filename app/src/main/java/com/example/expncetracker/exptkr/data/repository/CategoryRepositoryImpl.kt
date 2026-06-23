package com.example.expncetracker.exptkr.data.repository

import com.example.expncetracker.exptkr.data.db.dao.CategoryDao
import com.example.expncetracker.exptkr.data.db.entity.CategoryEntity
import com.example.expncetracker.exptkr.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {
    override fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    override suspend fun insertCategory(category: CategoryEntity) = categoryDao.insertCategory(category)
    override suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)
}
