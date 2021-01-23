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
        fun getInWorkIconDrawable() = R.drawable.ic_baseline_handyman_24
        fun getOnHoldIconDrawable() = R.drawable.ic_baseline_pause_24
        fun getDoneIconDrawable() = R.drawable.ic_baseline_done_24
    }
}