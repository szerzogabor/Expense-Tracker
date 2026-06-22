package com.expensetracker.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.expensetracker.domain.Period
import com.expensetracker.domain.PeriodType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val period: Period = Period(PeriodType.THIS_MONTH),
    val tagFilterAnd: Boolean = false,
    val includeFutureInReports: Boolean = false,
    val includePendingInReports: Boolean = false,
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val periodType = stringPreferencesKey("period_type")
        val customStart = longPreferencesKey("custom_start")
        val customEnd = longPreferencesKey("custom_end")
        val tagFilterAnd = booleanPreferencesKey("tag_filter_and")
        val includeFuture = booleanPreferencesKey("include_future")
        val includePending = booleanPreferencesKey("include_pending")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        val type = p[Keys.periodType]?.let { runCatching { PeriodType.valueOf(it) }.getOrNull() }
            ?: PeriodType.THIS_MONTH
        AppSettings(
            period = Period(type, p[Keys.customStart], p[Keys.customEnd]),
            tagFilterAnd = p[Keys.tagFilterAnd] ?: false,
            includeFutureInReports = p[Keys.includeFuture] ?: false,
            includePendingInReports = p[Keys.includePending] ?: false,
        )
    }

    suspend fun setPeriod(period: Period) {
        context.dataStore.edit { p ->
            p[Keys.periodType] = period.type.name
            if (period.customStart != null) p[Keys.customStart] = period.customStart
            if (period.customEnd != null) p[Keys.customEnd] = period.customEnd
        }
    }

    suspend fun setTagFilterAnd(value: Boolean) {
        context.dataStore.edit { it[Keys.tagFilterAnd] = value }
    }

    suspend fun setIncludeFuture(value: Boolean) {
        context.dataStore.edit { it[Keys.includeFuture] = value }
    }

    suspend fun setIncludePending(value: Boolean) {
        context.dataStore.edit { it[Keys.includePending] = value }
    }
}
