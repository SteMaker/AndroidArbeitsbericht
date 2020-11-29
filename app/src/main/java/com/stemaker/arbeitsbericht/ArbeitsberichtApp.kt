package com.stemaker.arbeitsbericht

import android.app.Application
import android.content.Context

class ArbeitsberichtApp: Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = getApplicationContext()
    }


    companion object {
        lateinit var appContext: Context
        fun getVersionCode(): Int {
            return appContext.packageManager.getPackageInfo(appContext.packageName!!, 0).versionCode
        }
    }
}