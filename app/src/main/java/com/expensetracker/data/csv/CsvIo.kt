package com.expensetracker.data.csv

import com.expensetracker.data.db.Account
import com.expensetracker.data.db.AppDatabase
import com.expensetracker.data.db.Category
import com.expensetracker.data.db.LearningRule
import com.expensetracker.data.db.Tag
import com.expensetracker.data.db.TransactionEntity
import com.expensetracker.data.db.TransactionTagCrossRef
import com.expensetracker.data.db.TransactionType

/**
 * Single-structure CSV import/export covering accounts, categories, tags,
 * learning rules and transactions (with tag names). Sections are delimited by
 * lines beginning with "#SECTION".
 */
class CsvIo(private val db: AppDatabase) {

    suspend fun export(): String {
        val sb = StringBuilder()
        val accounts = db.accountDao().getAll()
        val categories = db.categoryDao().getAll()
        val tags = db.tagDao().getAll()
        val rules = db.learningRuleDao().getAll()
        val txs = db.transactionDao().getAllWithTags()

        sb.appendLine("#SECTION accounts")
        sb.appendLine("id,name,icon,color,openingBalanceMinor,isDefault,isArchived,sortOrder")
        accounts.forEach {
            sb.appendLine(row(it.id, it.name, it.icon, it.color, it.openingBalanceMinor, b(it.isDefault), b(it.isArchived), it.sortOrder))
        }

        sb.appendLine("#SECTION categories")
        sb.appendLine("id,name,icon,color,sortOrder,isOther")
        categories.forEach {
            sb.appendLine(row(it.id, it.name, it.icon, it.color, it.sortOrder, b(it.isOther)))
        }

        sb.appendLine("#SECTION tags")
        sb.appendLine("id,name")
        tags.forEach { sb.appendLine(row(it.id, it.name)) }

        sb.appendLine("#SECTION learning")
        sb.appendLine("id,pattern,categoryId")
        rules.forEach { sb.appendLine(row(it.id, it.pattern, it.categoryId)) }

        sb.appendLine("#SECTION transactions")
        sb.appendLine("id,type,amountMinor,description,date,accountId,destAccountId,categoryId,note,isPending,tags")
        txs.forEach { t ->
            val tx = t.transaction
            val tagNames = t.tags.joinToString("|") { it.name }
            sb.appendLine(
                row(
                    tx.id, tx.type.name, tx.amountMinor, tx.description, tx.date,
                    tx.accountId, tx.destAccountId ?: "", tx.categoryId ?: "",
                    tx.note ?: "", b(tx.isPending), tagNames
                )
            )
        }
        return sb.toString()
    }

    suspend fun import(text: String, replace: Boolean) {
        val sections = parseSections(text)

        if (replace) {
            db.transactionDao().clearAllTagRefs()
            db.transactionDao().clear()
            db.learningRuleDao().clear()
            db.tagDao().clear()
            db.categoryDao().clear()
            db.accountDao().clear()
        }

        // id remaps used in merge mode
        val accMap = HashMap<Long, Long>()
        val catMap = HashMap<Long, Long>()
        val tagMap = HashMap<Long, Long>()

        // accounts
        sections["accounts"]?.forEach { f ->
            val oldId = f[0].toLong()
            val acc = Account(
                id = if (replace) oldId else 0,
                name = f[1], icon = f.getOrElse(2) { "wallet" },
                color = f[3].toLong(), openingBalanceMinor = f[4].toLong(),
                isDefault = f[5] == "1", isArchived = f[6] == "1", sortOrder = f[7].toInt()
            )
            val newId = db.accountDao().insert(acc)
            accMap[oldId] = if (replace) oldId else newId
        }

        // categories
        sections["categories"]?.forEach { f ->
            val oldId = f[0].toLong()
            val cat = Category(
                id = if (replace) oldId else 0,
                name = f[1], icon = f.getOrElse(2) { "category" },
                color = f[3].toLong(), sortOrder = f[4].toInt(), isOther = f[5] == "1"
            )
            val newId = db.categoryDao().insert(cat)
            catMap[oldId] = if (replace) oldId else newId
        }

        // tags
        sections["tags"]?.forEach { f ->
            val oldId = f[0].toLong()
            val existing = db.tagDao().getByName(f[1])
            val newId = existing?.id ?: db.tagDao().insert(Tag(id = if (replace) oldId else 0, name = f[1]))
            tagMap[oldId] = if (replace) oldId else newId
        }

        // learning rules
        sections["learning"]?.forEach { f ->
            val catOld = f[2].toLong()
            val catNew = catMap[catOld] ?: catOld
            db.learningRuleDao().insert(LearningRule(id = 0, pattern = f[1], categoryId = catNew))
        }

        // transactions
        sections["transactions"]?.forEach { f ->
            val oldAcc = f[5].toLong()
            val destOld = f[6].toLongOrNull()
            val catOld = f[7].toLongOrNull()
            val tx = TransactionEntity(
                id = if (replace) f[0].toLong() else 0,
                type = TransactionType.valueOf(f[1]),
                amountMinor = f[2].toLong(),
                description = f[3],
                date = f[4].toLong(),
                accountId = accMap[oldAcc] ?: oldAcc,
                destAccountId = destOld?.let { accMap[it] ?: it },
                categoryId = catOld?.let { catMap[it] ?: it },
                note = f[8].ifBlank { null },
                isPending = f[9] == "1",
            )
            val newTxId = db.transactionDao().insert(tx)
            val tagNames = f.getOrElse(10) { "" }
            if (tagNames.isNotBlank()) {
                tagNames.split("|").map { it.trim() }.filter { it.isNotEmpty() }.forEach { name ->
                    val tagId = db.tagDao().getByName(name)?.id ?: db.tagDao().insert(Tag(name = name)).let {
                        if (it == -1L) db.tagDao().getByName(name)!!.id else it
                    }
                    db.transactionDao().insertTagRef(TransactionTagCrossRef(newTxId, tagId))
                }
            }
        }
    }

    // ---- helpers ----
    private fun b(value: Boolean) = if (value) "1" else "0"

    private fun row(vararg fields: Any?): String =
        fields.joinToString(",") { escape(it?.toString() ?: "") }

    private fun escape(s: String): String =
        if (s.contains(',') || s.contains('"') || s.contains('\n')) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else s

    private fun parseSections(text: String): Map<String, List<List<String>>> {
        val result = LinkedHashMap<String, MutableList<List<String>>>()
        var current: String? = null
        var headerSeen = false
        text.split("\n").forEach { rawLine ->
            val line = rawLine.trimEnd('\r')
            when {
                line.startsWith("#SECTION") -> {
                    current = line.removePrefix("#SECTION").trim()
                    result[current!!] = mutableListOf()
                    headerSeen = false
                }
                line.isBlank() -> { /* skip */ }
                current != null -> {
                    if (!headerSeen) { headerSeen = true } // skip column header
                    else result[current]!!.add(parseLine(line))
                }
            }
        }
        return result
    }

    private fun parseLine(line: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes -> when {
                    c == '"' && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                    c == '"' -> inQuotes = false
                    else -> sb.append(c)
                }
                c == '"' -> inQuotes = true
                c == ',' -> { out.add(sb.toString()); sb.setLength(0) }
                else -> sb.append(c)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }
}
