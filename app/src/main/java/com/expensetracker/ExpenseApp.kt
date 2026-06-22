package com.expensetracker

import android.app.Application
import com.expensetracker.di.AppContainer

class ExpenseApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
