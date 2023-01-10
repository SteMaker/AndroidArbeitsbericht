package com.stemaker.arbeitsbericht

import android.app.AlertDialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.DialogInterface
import android.util.Log
import androidx.lifecycle.Observer
import androidx.room.Room
import com.google.gson.Gson
import com.stemaker.arbeitsbericht.data.*
import com.stemaker.arbeitsbericht.data.configuration.Configuration
import com.stemaker.arbeitsbericht.data.configuration.ConfigurationStore
import com.stemaker.arbeitsbericht.data.configuration.configuration
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.io.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "StorageHandler"

inline fun storageHandler() = StorageHandler

object StorageHandler {
    private val gson: Gson = Gson()

    private var _materialDictionary: MutableSet<String> = mutableSetOf<String>()
    private var materialDictionaryChanged: Boolean = false

    private var _workItemDictionary: MutableSet<String> = mutableSetOf<String>()
    private var workItemDictionaryChanged: Boolean = false

    val materialDictionary: Set<String>
        get() = _materialDictionary
    val workItemDictionary: Set<String>
        get() = _workItemDictionary

    fun loadConfigurationFromFile(c: Context): Boolean {
        try {
            val fIn = c.openFileInput("configuration.json")
            val isr = InputStreamReader(fIn)
            Configuration.store = gson.fromJson(isr, ConfigurationStore::class.java)
            // Only temp until I rework the configuration data handling to copy to/from json/db similar as for reports
            Configuration.reportIdPattern.value = Configuration.store.reportIdPattern
            return true // was able to read configuration
        }
        catch (e: Exception){
            return false // not able to read configuration -> no need to migrate to AbPreferences
        }
    }

    fun saveConfigurationToFile(c: Context) {
        configuration().materialDictionary = materialDictionary
        configuration().workItemDictionary = workItemDictionary
        // TODO: Save filter to configuration
        val fOut = c.openFileOutput("configuration.json", MODE_PRIVATE)
        val osw = OutputStreamWriter(fOut)
        gson.toJson(Configuration.store, osw)
        osw.close()
    }

    private fun addToMaterialDictionary(item: String) {
        if(_materialDictionary.add(item)) {
            materialDictionaryChanged = true
        }
    }

    private fun addToWorkItemDictionary(item: String) {
        if(_workItemDictionary.add(item)) {
            workItemDictionaryChanged = true
        }
    }

    // Das muss ich nach report repository schieben

}

