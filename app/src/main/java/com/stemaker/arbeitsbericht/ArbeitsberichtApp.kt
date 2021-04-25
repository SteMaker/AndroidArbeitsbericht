package com.stemaker.arbeitsbericht

import android.app.Application
import android.content.Context

class ArbeitsberichtApp: Application() {
    override fun onCreate() {
        super.onCreate()
        // Required for ApachePOI to work
        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl")
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl")
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl")
        appContext = applicationContext
    }

    companion object {
        lateinit var appContext: Context
        fun getVersionCode(): Int {
            return appContext.packageManager.getPackageInfo(appContext.packageName!!, 0).versionCode
        }
        fun getInWorkIconDrawable() = R.drawable.ic_baseline_handyman_24
        fun getOnHoldIconDrawable() = R.drawable.ic_baseline_pause_24
        fun getDoneIconDrawable() = R.drawable.ic_baseline_done_24
        fun getArchivedIconDrawable() = R.drawable.ic_baseline_archive_24
    }
}