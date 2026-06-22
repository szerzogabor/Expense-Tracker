package com.expensetracker.di

import android.content.Context
import com.expensetracker.data.csv.CsvIo
import com.expensetracker.data.db.AppDatabase
import com.expensetracker.data.prefs.SettingsStore
import com.expensetracker.data.repo.Repository

/** Simple manual dependency container (no Hilt to keep the build lean). */
class AppContainer(context: Context) {
    private val db = AppDatabase.get(context)
    val repository = Repository(db)
    val settingsStore = SettingsStore(context)
    val csvIo = CsvIo(db)
}
