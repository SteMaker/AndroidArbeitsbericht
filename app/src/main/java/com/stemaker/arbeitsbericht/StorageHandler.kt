package com.stemaker.arbeitsbericht

import android.content.Context
import com.google.gson.Gson
import com.stemaker.arbeitsbericht.data.configuration.Configuration
import com.stemaker.arbeitsbericht.data.configuration.ConfigurationStore
import java.io.*

private const val TAG = "StorageHandler"

object StorageHandler {
    private val gson: Gson = Gson()

    fun loadConfigurationFromFile(c: Context): Boolean {
        try {
            val fIn = c.openFileInput("configuration.json")
            val isr = InputStreamReader(fIn)
            Configuration.store = gson.fromJson(isr, ConfigurationStore::class.java)
            // Only temp until I rework the configuration data handling to copy to/from json/db similar as for reports
            Configuration.reportIdPattern.value = Configuration.store.reportIdPattern
            Configuration.updateConfiguration()
            return true // was able to read configuration
        }
        catch (e: Exception){
            return false // not able to read configuration -> no need to migrate to AbPreferences
        }
    }
    fun deleteConfigurationFile(c: Context) {
        c.deleteFile("configuration.json")
    }
}

