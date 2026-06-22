package com.expensetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.csv.CsvIo
import com.expensetracker.data.db.Account
import com.expensetracker.data.db.Category
import com.expensetracker.data.db.LearningRule
import com.expensetracker.data.db.RecurringRule
import com.expensetracker.data.db.Tag
import com.expensetracker.data.db.TransactionEntity
import com.expensetracker.data.prefs.AppSettings
import com.expensetracker.data.prefs.SettingsStore
import com.expensetracker.data.repo.Repository
import com.expensetracker.data.repo.TransactionForm
import com.expensetracker.di.AppContainer
import com.expensetracker.domain.AppData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    private val repository: Repository,
    private val settingsStore: SettingsStore,
    private val csvIo: CsvIo,
) : ViewModel() {

    val data: StateFlow<AppData> = repository.observeData()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppData())

    val settings: StateFlow<AppSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val recurring: StateFlow<List<RecurringRule>> = repository.observeRecurring()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val message = MutableStateFlow<String?>(null)
    fun clearMessage() { message.value = null }

    /** Filter consumed by the transaction list when opened from a category/tag/account. */
    val pendingFilter = MutableStateFlow<TxFilter?>(null)
    fun setFilter(filter: TxFilter?) { pendingFilter.value = filter }

    /** Seed values for the transaction editor (new / duplicate / reuse). */
    val editorSeed = MutableStateFlow<TransactionForm?>(null)
    fun setEditorSeed(seed: TransactionForm?) { editorSeed.value = seed }

    init {
        viewModelScope.launch {
            runCatching {
                repository.ensureSeed()
                val n = repository.generateDue()
                if (n > 0) message.value = "Generated $n recurring transaction(s)"
            }.onFailure { message.value = "Startup warning: ${it.message}" }
        }
    }

    // ---- transactions ----
    fun saveTransaction(form: TransactionForm, onDone: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = repository.saveTransaction(form)
        onDone(id)
    }

    fun saveTransactionScoped(form: TransactionForm, scope: Repository.EditScope, onDone: () -> Unit = {}) = viewModelScope.launch {
        repository.saveTransactionScoped(form, scope)
        onDone()
    }

    fun deleteTransaction(tx: TransactionEntity) = viewModelScope.launch {
        repository.deleteTransaction(tx)
    }

    fun approvePending(tx: TransactionEntity) = viewModelScope.launch {
        repository.approvePending(tx)
    }

    fun detachOccurrence(tx: TransactionEntity) = viewModelScope.launch {
        repository.detachOccurrence(tx)
    }

    // ---- accounts ----
    fun addAccount(account: Account) = viewModelScope.launch { repository.addAccount(account) }
    fun updateAccount(account: Account) = viewModelScope.launch { repository.updateAccount(account) }
    fun setDefaultAccount(id: Long) = viewModelScope.launch { repository.setDefaultAccount(id) }
    fun archiveAccount(id: Long, newDefaultId: Long?) = viewModelScope.launch { repository.archiveAccount(id, newDefaultId) }
    fun unarchiveAccount(id: Long) = viewModelScope.launch { repository.unarchiveAccount(id) }
    fun reorderAccounts(ordered: List<Account>) = viewModelScope.launch { repository.reorderAccounts(ordered) }

    // ---- categories ----
    fun addCategory(category: Category) = viewModelScope.launch { repository.addCategory(category) }
    fun updateCategory(category: Category) = viewModelScope.launch { repository.updateCategory(category) }
    fun deleteCategory(category: Category) = viewModelScope.launch { repository.deleteCategory(category) }
    fun reorderCategories(ordered: List<Category>) = viewModelScope.launch { repository.reorderCategories(ordered) }

    // ---- tags ----
    fun deleteTag(tag: Tag) = viewModelScope.launch { repository.deleteTag(tag) }

    // ---- learning ----
    fun addLearningRule(rule: LearningRule) = viewModelScope.launch { repository.addLearningRule(rule) }
    fun updateLearningRule(rule: LearningRule) = viewModelScope.launch { repository.updateLearningRule(rule) }
    fun deleteLearningRule(rule: LearningRule) = viewModelScope.launch { repository.deleteLearningRule(rule) }

    // ---- recurring ----
    fun addRecurring(rule: RecurringRule) = viewModelScope.launch {
        repository.addRecurring(rule)
        repository.generateDue()
    }
    fun updateRecurring(rule: RecurringRule) = viewModelScope.launch { repository.updateRecurring(rule) }
    fun deleteRecurring(rule: RecurringRule) = viewModelScope.launch { repository.deleteRecurring(rule) }

    // ---- settings ----
    fun setPeriod(period: com.expensetracker.domain.Period) = viewModelScope.launch { settingsStore.setPeriod(period) }
    fun setTagFilterAnd(v: Boolean) = viewModelScope.launch { settingsStore.setTagFilterAnd(v) }
    fun setIncludeFuture(v: Boolean) = viewModelScope.launch { settingsStore.setIncludeFuture(v) }
    fun setIncludePending(v: Boolean) = viewModelScope.launch { settingsStore.setIncludePending(v) }

    // ---- CSV ----
    suspend fun exportCsv(): String = csvIo.export()
    fun importCsv(text: String, replace: Boolean) = viewModelScope.launch {
        runCatching { csvIo.import(text, replace) }
            .onSuccess {
                repository.ensureSeed()
                message.value = if (replace) "Data replaced from CSV" else "Data merged from CSV"
            }
            .onFailure { message.value = "Import failed: ${it.message}" }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AppViewModel(container.repository, container.settingsStore, container.csvIo) as T
            }
    }
}
