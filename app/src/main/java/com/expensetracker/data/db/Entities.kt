package com.expensetracker.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Money is stored as minor units (value * 100) to avoid floating point drift. */

enum class TransactionType { EXPENSE, INCOME, TRANSFER, ADJUSTMENT }

enum class GenerationMode { AUTO, PENDING }

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "wallet",
    val color: Long = 0xFF1B5E20,
    val openingBalanceMinor: Long = 0,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "category",
    val color: Long = 0xFF607D8B,
    val sortOrder: Int = 0,
    val isOther: Boolean = false,
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "transactions",
    indices = [Index("accountId"), Index("categoryId"), Index("date"), Index("recurringRuleId")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    /** Positive magnitude for EXPENSE/INCOME/TRANSFER; signed delta for ADJUSTMENT. */
    val amountMinor: Long,
    val description: String,
    /** Date stored as epoch day (date only, no time). */
    val date: Long,
    val accountId: Long,
    val destAccountId: Long? = null,
    val categoryId: Long? = null,
    val note: String? = null,
    val isPending: Boolean = false,
    val recurringRuleId: Long? = null,
    val occurrenceDate: Long? = null,
)

@Entity(
    tableName = "transaction_tags",
    primaryKeys = ["transactionId", "tagId"],
    indices = [Index("tagId")]
)
data class TransactionTagCrossRef(
    val transactionId: Long,
    val tagId: Long,
)

@Entity(tableName = "learning_rules")
data class LearningRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Case-insensitive substring matched against the transaction description. */
    val pattern: String,
    val categoryId: Long,
)

@Entity(tableName = "recurring_rules")
data class RecurringRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** RFC-5545 RRULE body, e.g. "FREQ=MONTHLY;INTERVAL=1". */
    val rrule: String,
    val startDate: Long,
    val endDate: Long? = null,
    val generationMode: GenerationMode = GenerationMode.AUTO,
    val enabled: Boolean = true,
    val lastGeneratedDate: Long? = null,
    // Template for generated transactions
    val type: TransactionType,
    val amountMinor: Long,
    val description: String,
    val accountId: Long,
    val destAccountId: Long? = null,
    val categoryId: Long? = null,
    val note: String? = null,
    /** Comma-separated tag names applied to generated transactions. */
    val templateTags: String = "",
)

@Entity(tableName = "recurring_exceptions", primaryKeys = ["ruleId", "date"])
data class RecurringException(
    val ruleId: Long,
    /** Occurrence epoch day to skip (was detached/edited individually). */
    val date: Long,
)
