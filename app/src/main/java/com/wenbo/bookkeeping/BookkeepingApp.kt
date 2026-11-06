package com.wenbo.bookkeeping

import android.app.Application
import com.wenbo.bookkeeping.di.AppContainer

class BookkeepingApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
