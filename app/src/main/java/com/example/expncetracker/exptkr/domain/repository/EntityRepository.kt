package com.example.expncetracker.exptkr.domain.repository

import com.example.expncetracker.exptkr.domain.model.Category
import kotlinx.coroutines.flow.StateFlow

interface EntityRepository {
    val categories: StateFlow<List<Category>>
}
