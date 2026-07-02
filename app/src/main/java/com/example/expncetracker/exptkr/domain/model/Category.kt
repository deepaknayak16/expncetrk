package com.example.expncetracker.exptkr.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val name: String,
    val type: String,
    val icon: String,
    val color: String
)
