package com.stemaker.arbeitsbericht

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log

class ArbeitsberichtApp: Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = getApplicationContext()
    }

    companion object {
        lateinit var appContext: Context
    }
}