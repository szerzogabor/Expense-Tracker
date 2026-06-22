package com.expensetracker.domain

import com.expensetracker.data.db.Account
import com.expensetracker.data.db.Category
import com.expensetracker.data.db.LearningRule
import com.expensetracker.data.db.Tag
import com.expensetracker.data.db.TransactionEntity
import com.expensetracker.data.db.TransactionType

/** A transaction with its related entities resolved for display. */
data class UiTransaction(
    val tx: TransactionEntity,
    val account: Account?,
    val destAccount: Account?,
    val category: Category?,
    val tags: List<Tag>,
)

/** Immutable snapshot of all data used to derive every screen. */
data class AppData(
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val transactions: List<UiTransaction> = emptyList(),
    val learningRules: List<LearningRule> = emptyList(),
) {
    val activeAccounts get() = accounts.filter { !it.isArchived }
    fun account(id: Long?) = accounts.firstOrNull { it.id == id }
    fun category(id: Long?) = categories.firstOrNull { it.id == id }
    val defaultAccount get() = activeAccounts.firstOrNull { it.isDefault } ?: activeAccounts.firstOrNull()
}

data class CategorySummary(
    val category: Category,
    val totalMinor: Long,
    val count: Int,
)

data class TagSummary(
    val tag: Tag,
    val totalMinor: Long,
    val count: Int,
)

data class AccountTotals(
    val balanceMinor: Long,
    val incomeMinor: Long,
    val expenseMinor: Long,
    val transferInMinor: Long,
    val transferOutMinor: Long,
)

object Analytics {

    /** Calculated balance per account id. Pending transactions are excluded. */
    fun accountBalances(data: AppData): Map<Long, Long> {
        val result = HashMap<Long, Long>()
        for (a in data.accounts) result[a.id] = a.openingBalanceMinor
        for (ui in data.transactions) {
            val t = ui.tx
            if (t.isPending) continue
            when (t.type) {
                TransactionType.EXPENSE -> result.merge(t.accountId, -t.amountMinor, Long::plus)
                TransactionType.INCOME -> result.merge(t.accountId, t.amountMinor, Long::plus)
                TransactionType.ADJUSTMENT -> result.merge(t.accountId, t.amountMinor, Long::plus)
                TransactionType.TRANSFER -> {
                    result.merge(t.accountId, -t.amountMinor, Long::plus)
                    t.destAccountId?.let { result.merge(it, t.amountMinor, Long::plus) }
                }
            }
        }
        return result
    }

    fun totalActiveBalance(data: AppData): Long {
        val balances = accountBalances(data)
        return data.activeAccounts.sumOf { balances[it.id] ?: 0L }
    }

    private fun inRange(ui: UiTransaction, range: Pair<Long?, Long?>, includePending: Boolean, includeFuture: Boolean, todayEpoch: Long): Boolean {
        if (!includePending && ui.tx.isPending) return false
        if (!includeFuture && ui.tx.date > todayEpoch) return false
        return Period.contains(range, ui.tx.date)
    }

    fun filterForReports(
        data: AppData,
        range: Pair<Long?, Long?>,
        includePending: Boolean,
        includeFuture: Boolean,
        todayEpoch: Long,
    ): List<UiTransaction> =
        data.transactions.filter { inRange(it, range, includePending, includeFuture, todayEpoch) }

    fun income(txs: List<UiTransaction>): Long =
        txs.filter { it.tx.type == TransactionType.INCOME }.sumOf { it.tx.amountMinor }

    fun expense(txs: List<UiTransaction>): Long =
        txs.filter { it.tx.type == TransactionType.EXPENSE }.sumOf { it.tx.amountMinor }

    /** Category summary over EXPENSE transactions (income excluded), with Other fallback. */
    fun categorySummaries(
        txs: List<UiTransaction>,
        data: AppData,
        type: TransactionType = TransactionType.EXPENSE,
    ): List<CategorySummary> {
        val other = data.categories.firstOrNull { it.isOther }
        val map = LinkedHashMap<Long, Pair<Long, Int>>()
        for (ui in txs) {
            if (ui.tx.type != type) continue
            val cat = ui.category ?: other ?: continue
            val cur = map[cat.id] ?: (0L to 0)
            map[cat.id] = (cur.first + ui.tx.amountMinor) to (cur.second + 1)
        }
        return map.entries.mapNotNull { (id, v) ->
            val cat = data.category(id) ?: return@mapNotNull null
            CategorySummary(cat, v.first, v.second)
        }
    }

    /** Tag summary: full amount counts toward every tag on a transaction. */
    fun tagSummaries(
        txs: List<UiTransaction>,
        type: TransactionType? = TransactionType.EXPENSE,
    ): List<TagSummary> {
        val map = LinkedHashMap<Long, Triple<Tag, Long, Int>>()
        for (ui in txs) {
            if (type != null && ui.tx.type != type) continue
            for (tag in ui.tags) {
                val cur = map[tag.id]
                map[tag.id] = if (cur == null) Triple(tag, ui.tx.amountMinor, 1)
                else Triple(tag, cur.second + ui.tx.amountMinor, cur.third + 1)
            }
        }
        return map.values.map { TagSummary(it.first, it.second, it.third) }
    }

    fun accountTotals(
        data: AppData,
        accountId: Long,
        range: Pair<Long?, Long?>,
        includePending: Boolean,
        includeFuture: Boolean,
        todayEpoch: Long,
    ): AccountTotals {
        var income = 0L; var expense = 0L; var tin = 0L; var tout = 0L
        for (ui in data.transactions) {
            val t = ui.tx
            if (!includePending && t.isPending) continue
            if (!includeFuture && t.date > todayEpoch) continue
            if (!Period.contains(range, t.date)) continue
            when (t.type) {
                TransactionType.INCOME -> if (t.accountId == accountId) income += t.amountMinor
                TransactionType.EXPENSE -> if (t.accountId == accountId) expense += t.amountMinor
                TransactionType.ADJUSTMENT -> { /* affects balance only */ }
                TransactionType.TRANSFER -> {
                    if (t.accountId == accountId) tout += t.amountMinor
                    if (t.destAccountId == accountId) tin += t.amountMinor
                }
            }
        }
        val balance = accountBalances(data)[accountId] ?: 0L
        return AccountTotals(balance, income, expense, tin, tout)
    }

    /** Suggest a category from learning rules by matching the description (substring, case-insensitive). */
    fun suggestCategory(description: String, rules: List<LearningRule>): Long? {
        val desc = description.lowercase()
        if (desc.isBlank()) return null
        return rules.firstOrNull { it.pattern.isNotBlank() && desc.contains(it.pattern.lowercase()) }?.categoryId
    }
}
