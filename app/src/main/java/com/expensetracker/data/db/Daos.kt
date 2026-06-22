package com.expensetracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert suspend fun insert(account: Account): Long
    @Update suspend fun update(account: Account)
    @Delete suspend fun delete(account: Account)

    @Query("SELECT * FROM accounts ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<Account>>

    @Query("SELECT * FROM accounts ORDER BY sortOrder, name")
    suspend fun getAll(): List<Account>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): Account?

    @Query("UPDATE accounts SET isDefault = 0")
    suspend fun clearDefault()

    @Query("SELECT MAX(sortOrder) FROM accounts")
    suspend fun maxSortOrder(): Int?

    @Query("DELETE FROM accounts")
    suspend fun clear()
}

@Dao
interface CategoryDao {
    @Insert suspend fun insert(category: Category): Long
    @Update suspend fun update(category: Category)
    @Delete suspend fun delete(category: Category)

    @Query("SELECT * FROM categories ORDER BY isOther, sortOrder, name")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY isOther, sortOrder, name")
    suspend fun getAll(): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): Category?

    @Query("SELECT * FROM categories WHERE isOther = 1 LIMIT 1")
    suspend fun getOther(): Category?

    @Query("SELECT MAX(sortOrder) FROM categories")
    suspend fun maxSortOrder(): Int?

    @Query("UPDATE transactions SET categoryId = :toId WHERE categoryId = :fromId")
    suspend fun reassignCategory(fromId: Long, toId: Long?)

    @Query("DELETE FROM categories")
    suspend fun clear()
}

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(tag: Tag): Long
    @Delete suspend fun delete(tag: Tag)

    @Query("SELECT * FROM tags ORDER BY name")
    fun observeAll(): Flow<List<Tag>>

    @Query("SELECT * FROM tags ORDER BY name")
    suspend fun getAll(): List<Tag>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): Tag?

    @Query("DELETE FROM tags")
    suspend fun clear()
}

@Dao
interface TransactionDao {
    @Insert suspend fun insert(tx: TransactionEntity): Long
    @Update suspend fun update(tx: TransactionEntity)
    @Delete suspend fun delete(tx: TransactionEntity)

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAllWithTags(): Flow<List<TransactionWithTags>>

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    suspend fun getAllWithTags(): List<TransactionWithTags>

    @Query("SELECT * FROM transactions ORDER BY date, id")
    suspend fun getAllOrderedByDate(): List<TransactionEntity>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getWithTags(id: Long): TransactionWithTags?

    // --- tag cross refs ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTagRef(ref: TransactionTagCrossRef)

    @Query("DELETE FROM transaction_tags WHERE transactionId = :txId")
    suspend fun clearTagRefs(txId: Long)

    @Query("SELECT * FROM transaction_tags")
    suspend fun getAllTagRefs(): List<TransactionTagCrossRef>

    @Query("DELETE FROM transactions")
    suspend fun clear()

    @Query("DELETE FROM transaction_tags")
    suspend fun clearAllTagRefs()
}

@Dao
interface LearningRuleDao {
    @Insert suspend fun insert(rule: LearningRule): Long
    @Update suspend fun update(rule: LearningRule)
    @Delete suspend fun delete(rule: LearningRule)

    @Query("SELECT * FROM learning_rules")
    fun observeAll(): Flow<List<LearningRule>>

    @Query("SELECT * FROM learning_rules")
    suspend fun getAll(): List<LearningRule>

    @Query("DELETE FROM learning_rules")
    suspend fun clear()
}

@Dao
interface RecurringDao {
    @Insert suspend fun insert(rule: RecurringRule): Long
    @Update suspend fun update(rule: RecurringRule)
    @Delete suspend fun delete(rule: RecurringRule)

    @Query("SELECT * FROM recurring_rules ORDER BY id DESC")
    fun observeAll(): Flow<List<RecurringRule>>

    @Query("SELECT * FROM recurring_rules")
    suspend fun getAll(): List<RecurringRule>

    @Query("SELECT * FROM recurring_rules WHERE id = :id")
    suspend fun getById(id: Long): RecurringRule?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertException(ex: RecurringException)

    @Query("SELECT * FROM recurring_exceptions WHERE ruleId = :ruleId")
    suspend fun getExceptions(ruleId: Long): List<RecurringException>

    @Query("DELETE FROM recurring_rules")
    suspend fun clear()

    @Query("DELETE FROM recurring_exceptions")
    suspend fun clearExceptions()
}
