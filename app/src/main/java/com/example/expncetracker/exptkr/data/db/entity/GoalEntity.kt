package com.example.expncetracker.exptkr.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.expncetracker.exptkr.data.db.entity.AccountEntity

@Entity(
    tableName = "goals",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["linked_account_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["linked_account_id"])]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: java.math.BigDecimal,
    val currentAmount: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    val deadline: Long? = null,
    val iconName: String = "SAVINGS",
    val color: Int,
    val isCompleted: Boolean = false,

    // NEW: Which category auto-contributes to this goal
    @ColumnInfo(name = "linked_category")
    val linkedCategory: String? = null, // e.g. "Savings", "Investment"

    // NEW: Which account holds the "saved" money for this goal
    @ColumnInfo(name = "linked_account_id")
    val linkedAccountId: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
