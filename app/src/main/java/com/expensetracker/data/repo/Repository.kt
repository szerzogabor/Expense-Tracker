package com.expensetracker.data.repo

import com.expensetracker.data.db.Account
import com.expensetracker.data.db.AppDatabase
import com.expensetracker.data.db.Category
import com.expensetracker.data.db.GenerationMode
import com.expensetracker.data.db.LearningRule
import com.expensetracker.data.db.RecurringException
import com.expensetracker.data.db.RecurringRule
import com.expensetracker.data.db.Tag
import com.expensetracker.data.db.TransactionEntity
import com.expensetracker.data.db.TransactionTagCrossRef
import com.expensetracker.data.db.TransactionType
import com.expensetracker.domain.AppData
import com.expensetracker.domain.RecurrenceEngine
import com.expensetracker.domain.UiTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/** Data used to create or update a transaction. */
data class TransactionForm(
    val id: Long = 0,
    val type: TransactionType,
    val amountMinor: Long,
    val description: String,
    val date: Long,
    val accountId: Long,
    val destAccountId: Long? = null,
    val categoryId: Long? = null,
    val note: String? = null,
    val tagNames: List<String> = emptyList(),
    val isPending: Boolean = false,
    val recurringRuleId: Long? = null,
    val occurrenceDate: Long? = null,
)

class Repository(private val db: AppDatabase) {

    private val accountDao = db.accountDao()
    private val categoryDao = db.categoryDao()
    private val tagDao = db.tagDao()
    private val txDao = db.transactionDao()
    private val learnDao = db.learningRuleDao()
    private val recurDao = db.recurringDao()

    fun observeData(): Flow<AppData> = combine(
        txDao.observeAllWithTags(),
        accountDao.observeAll(),
        categoryDao.observeAll(),
        tagDao.observeAll(),
        learnDao.observeAll(),
    ) { txs, accounts, categories, tags, rules ->
        val accMap = accounts.associateBy { it.id }
        val catMap = categories.associateBy { it.id }
        val other = categories.firstOrNull { it.isOther }
        val ui = txs.map { t ->
            UiTransaction(
                tx = t.transaction,
                account = accMap[t.transaction.accountId],
                destAccount = t.transaction.destAccountId?.let { accMap[it] },
                category = t.transaction.categoryId?.let { catMap[it] } ?: other,
                tags = t.tags.sortedBy { it.name },
            )
        }
        AppData(accounts, categories, tags, ui, rules)
    }

    fun observeRecurring(): Flow<List<RecurringRule>> = recurDao.observeAll()

    // ---- seed ----
    suspend fun ensureSeed() {
        if (categoryDao.getOther() == null) {
            categoryDao.insert(Category(name = "Other", icon = "category", color = 0xFF9E9E9E, sortOrder = 9999, isOther = true))
        }
        if (accountDao.getAll().isEmpty()) {
            accountDao.insert(Account(name = "Cash", icon = "cash", color = 0xFF2E7D32, isDefault = true, sortOrder = 0))
        }
        if (categoryDao.getAll().none { !it.isOther }) {
            val defaults = listOf(
                "Groceries" to 0xFF388E3C, "Transport" to 0xFF1976D2, "Dining" to 0xFFE64A19,
                "Bills" to 0xFF7B1FA2, "Shopping" to 0xFFC2185B, "Health" to 0xFF00897B,
                "Salary" to 0xFF2E7D32,
            )
            defaults.forEachIndexed { i, (name, color) ->
                categoryDao.insert(Category(name = name, color = color, sortOrder = i))
            }
        }
    }

    // ---- transactions ----
    suspend fun saveTransaction(form: TransactionForm): Long {
        val entity = TransactionEntity(
            id = form.id,
            type = form.type,
            amountMinor = form.amountMinor,
            description = form.description.trim(),
            date = form.date,
            accountId = form.accountId,
            destAccountId = if (form.type == TransactionType.TRANSFER) form.destAccountId else null,
            categoryId = if (form.type == TransactionType.EXPENSE || form.type == TransactionType.INCOME) form.categoryId else null,
            note = form.note?.trim()?.ifBlank { null },
            isPending = form.isPending,
            recurringRuleId = form.recurringRuleId,
            occurrenceDate = form.occurrenceDate,
        )
        val id = if (form.id == 0L) txDao.insert(entity) else {
            txDao.update(entity); form.id
        }
        // tags
        txDao.clearTagRefs(id)
        for (name in form.tagNames.map { it.trim() }.filter { it.isNotEmpty() }.distinct()) {
            val tagId = upsertTag(name)
            txDao.insertTagRef(TransactionTagCrossRef(id, tagId))
        }
        // auto-learn category mapping from description
        if (entity.categoryId != null && entity.description.isNotBlank()) {
            autoLearn(entity.description, entity.categoryId)
        }
        return id
    }

    enum class EditScope { THIS, THIS_AND_FUTURE, ALL }

    /** Save an edit to a recurring-linked transaction, honouring the chosen scope. */
    suspend fun saveTransactionScoped(form: TransactionForm, scope: EditScope): Long {
        val ruleId = form.recurringRuleId
        val occ = form.occurrenceDate
        if (ruleId == null) return saveTransaction(form)
        val rule = recurDao.getById(ruleId)
        return when (scope) {
            EditScope.THIS -> {
                if (occ != null) recurDao.insertException(RecurringException(ruleId, occ))
                saveTransaction(form.copy(recurringRuleId = null, occurrenceDate = null))
            }
            EditScope.ALL -> {
                rule?.let {
                    recurDao.update(
                        it.copy(
                            type = form.type, amountMinor = form.amountMinor,
                            description = form.description, accountId = form.accountId,
                            destAccountId = form.destAccountId, categoryId = form.categoryId,
                            note = form.note, templateTags = form.tagNames.joinToString(",")
                        )
                    )
                }
                saveTransaction(form)
            }
            EditScope.THIS_AND_FUTURE -> {
                if (rule != null && occ != null) {
                    recurDao.update(rule.copy(endDate = occ - 1))
                    val newId = recurDao.insert(
                        rule.copy(
                            id = 0, startDate = occ, endDate = rule.endDate, lastGeneratedDate = occ,
                            type = form.type, amountMinor = form.amountMinor,
                            description = form.description, accountId = form.accountId,
                            destAccountId = form.destAccountId, categoryId = form.categoryId,
                            note = form.note, templateTags = form.tagNames.joinToString(",")
                        )
                    )
                    saveTransaction(form.copy(recurringRuleId = newId))
                } else saveTransaction(form)
            }
        }
    }

    suspend fun deleteTransaction(tx: TransactionEntity) {
        txDao.clearTagRefs(tx.id)
        txDao.delete(tx)
    }

    suspend fun approvePending(tx: TransactionEntity) {
        txDao.update(tx.copy(isPending = false))
    }

    private suspend fun upsertTag(name: String): Long {
        tagDao.getByName(name)?.let { return it.id }
        val id = tagDao.insert(Tag(name = name))
        return if (id == -1L) tagDao.getByName(name)!!.id else id
    }

    private suspend fun autoLearn(description: String, categoryId: Long) {
        val token = description.trim().split(Regex("\\s+")).firstOrNull()?.takeIf { it.length >= 2 } ?: return
        val existing = learnDao.getAll().firstOrNull { it.pattern.equals(token, ignoreCase = true) }
        if (existing == null) {
            learnDao.insert(LearningRule(pattern = token, categoryId = categoryId))
        } else if (existing.categoryId != categoryId) {
            learnDao.update(existing.copy(categoryId = categoryId))
        }
    }

    // ---- accounts ----
    suspend fun addAccount(account: Account): Long {
        val order = (accountDao.maxSortOrder() ?: -1) + 1
        var toSave = account.copy(sortOrder = order)
        if (accountDao.getAll().none { !it.isArchived }) toSave = toSave.copy(isDefault = true)
        if (toSave.isDefault) accountDao.clearDefault()
        return accountDao.insert(toSave)
    }

    suspend fun updateAccount(account: Account) {
        if (account.isDefault) {
            accountDao.clearDefault()
        }
        accountDao.update(account)
    }

    suspend fun setDefaultAccount(id: Long) {
        accountDao.clearDefault()
        accountDao.getById(id)?.let { accountDao.update(it.copy(isDefault = true, isArchived = false)) }
    }

    /** Archive an account; if it was the default, [newDefaultId] becomes default. */
    suspend fun archiveAccount(id: Long, newDefaultId: Long?) {
        val acc = accountDao.getById(id) ?: return
        accountDao.update(acc.copy(isArchived = true, isDefault = false))
        if (acc.isDefault && newDefaultId != null) setDefaultAccount(newDefaultId)
    }

    suspend fun unarchiveAccount(id: Long) {
        accountDao.getById(id)?.let { accountDao.update(it.copy(isArchived = false)) }
    }

    suspend fun reorderAccounts(ordered: List<Account>) {
        ordered.forEachIndexed { i, a -> accountDao.update(a.copy(sortOrder = i)) }
    }

    // ---- categories ----
    suspend fun addCategory(category: Category): Long {
        val order = (categoryDao.maxSortOrder() ?: -1) + 1
        return categoryDao.insert(category.copy(sortOrder = order, isOther = false))
    }

    suspend fun updateCategory(category: Category) = categoryDao.update(category)

    suspend fun deleteCategory(category: Category) {
        if (category.isOther) return
        val other = categoryDao.getOther()
        categoryDao.reassignCategory(category.id, other?.id)
        // clean learning rules pointing at deleted category
        learnDao.getAll().filter { it.categoryId == category.id }.forEach { learnDao.delete(it) }
        categoryDao.delete(category)
    }

    suspend fun reorderCategories(ordered: List<Category>) {
        ordered.forEachIndexed { i, c -> if (!c.isOther) categoryDao.update(c.copy(sortOrder = i)) }
    }

    // ---- tags ----
    suspend fun deleteTag(tag: Tag) = tagDao.delete(tag)

    // ---- learning rules ----
    suspend fun addLearningRule(rule: LearningRule) { learnDao.insert(rule) }
    suspend fun updateLearningRule(rule: LearningRule) = learnDao.update(rule)
    suspend fun deleteLearningRule(rule: LearningRule) = learnDao.delete(rule)

    // ---- recurring ----
    suspend fun addRecurring(rule: RecurringRule): Long = recurDao.insert(rule)
    suspend fun updateRecurring(rule: RecurringRule) = recurDao.update(rule)
    suspend fun getRecurring(id: Long) = recurDao.getById(id)

    suspend fun deleteRecurring(rule: RecurringRule) = recurDao.delete(rule)

    suspend fun getExceptions(ruleId: Long) = recurDao.getExceptions(ruleId)

    /** "This and future": end current rule before [fromEpoch], start a new rule with [newTemplate]. */
    suspend fun splitRecurring(rule: RecurringRule, fromEpoch: Long, newTemplate: RecurringRule): Long {
        recurDao.update(rule.copy(endDate = fromEpoch - 1))
        return recurDao.insert(newTemplate.copy(id = 0, startDate = fromEpoch))
    }

    /** "This occurrence only": detach the given transaction and skip its date in the rule. */
    suspend fun detachOccurrence(tx: TransactionEntity) {
        val ruleId = tx.recurringRuleId
        val occ = tx.occurrenceDate
        if (ruleId != null && occ != null) {
            recurDao.insertException(RecurringException(ruleId, occ))
        }
        txDao.update(tx.copy(recurringRuleId = null, occurrenceDate = null))
    }

    /** Generate due occurrences up to [today]. Returns number generated. */
    suspend fun generateDue(today: LocalDate = LocalDate.now()): Int {
        val todayEpoch = today.toEpochDay()
        var generated = 0
        for (rule in recurDao.getAll()) {
            if (!rule.enabled) continue
            val from = (rule.lastGeneratedDate?.plus(1)) ?: rule.startDate
            val to = minOf(todayEpoch, rule.endDate ?: todayEpoch)
            if (to < from) continue
            val exceptions = recurDao.getExceptions(rule.id).map { it.date }.toSet()
            val occ = RecurrenceEngine.occurrencesBetween(rule.rrule, rule.startDate, from, to, exceptions)
            for (ep in occ) {
                val form = TransactionForm(
                    type = rule.type,
                    amountMinor = rule.amountMinor,
                    description = rule.description,
                    date = ep,
                    accountId = rule.accountId,
                    destAccountId = rule.destAccountId,
                    categoryId = rule.categoryId,
                    note = rule.note,
                    tagNames = rule.templateTags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    isPending = rule.generationMode == GenerationMode.PENDING,
                    recurringRuleId = rule.id,
                    occurrenceDate = ep,
                )
                saveTransaction(form)
                generated++
            }
            recurDao.update(rule.copy(lastGeneratedDate = to))
        }
        return generated
    }
}
