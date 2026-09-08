package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val category: String,
    val notes: String = "",
    val date: Long = System.currentTimeMillis(),
    val currency: String = "USD",
    val isTaxDeductible: Boolean = false,
    val taxCategory: String = "",
    val receiptNotes: String = "",
    val receiptImagePath: String = ""
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val currency: String = "USD",
    val billingCycle: String = "MONTHLY", // "WEEKLY", "MONTHLY", "YEARLY"
    val nextDueDate: Long = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L,
    val category: String = "Subscriptions",
    val notes: String = "",
    val isActive: Boolean = true,
    val reminderDaysBefore: Int = 3
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val monthlyLimit: Double,
    val monthYear: String, // e.g. "2026-09"
    val currency: String = "USD"
)
