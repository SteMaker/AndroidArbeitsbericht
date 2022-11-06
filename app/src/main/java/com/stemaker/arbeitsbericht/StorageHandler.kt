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
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.io.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "StorageHandler"

inline fun storageHandler() = StorageHandler

// This only reflects reports which are visible due to active filters
// visReportIds only reflects those
// reports is a general cache that at the moment simply remembers everything that was ever read, except reports that have been deleted
interface ReportListObserver {
    fun notifyReportAdded(cnt: Int)
    fun notifyReportRemoved(cnt: Int)
    fun notifyReportListChanged(cnts: List<Int>)
}
object StorageHandler {
    private val gson: Gson = Gson()
    private val visReportCnts: MutableList<Int> = mutableListOf()
    private var activeReport: ReportData? = null
    private val reports: MutableMap<Int, ReportData> = mutableMapOf()

    private var _materialDictionary: MutableSet<String> = mutableSetOf<String>()
    private var materialDictionaryChanged: Boolean = false

    private var _workItemDictionary: MutableSet<String> = mutableSetOf<String>()
    private var workItemDictionaryChanged: Boolean = false

    val materialDictionary: Set<String>
        get() = _materialDictionary
    val workItemDictionary: Set<String>
        get() = _workItemDictionary


    private val inited = AtomicBoolean(false)
    val mutex = Mutex()

    var reportListObservers = mutableListOf<ReportListObserver>()


    private suspend fun checkAndRepairCurrentIdConsistency(activityContext: Context) {
        val usedIds = db.reportDao().getReportCnts()
        val maxId = usedIds.maxOrNull() ?:0 // If file didn't exist yet, initial value will be 1
        if(maxId >= configuration().currentId) {
            Log.e(TAG, "currentId of configuration is less than max Report ID (cnt). Seems the configuration was lost.")
            configuration().currentId = maxId+1
            configuration().save()
            Log.e(TAG, "Setting currentId to ${configuration().currentId} as minimum repair mechanism")
            AlertDialog.Builder(activityContext)
                .setCancelable(false)
                .setMessage("Entschuldigung, es wurde eine Dateninkonsistenz in den Einstellungen festgestellt. Bitte prüfen Sie die Einstellungen in der App.")
                .setPositiveButton("OK") { dialog, _ -> dialog.cancel() }
                .show()
        }
    }

    private fun fetchAllCntsFromDb() {
        GlobalScope.launch(Dispatchers.Main) {
            val cnts = withContext(Dispatchers.IO) {
                db.reportDao().getFilteredReportIds(configuration().reportFilter)
            }
            synchronized(visReportCnts) {
                visReportCnts.clear()
                visReportCnts.addAll(cnts)
            }
            for (o in reportListObservers)
                o.notifyReportListChanged(visReportCnts)
        }
    }

    fun addReportListObserver(obs: ReportListObserver) {
        reportListObservers.add(obs)
        obs.notifyReportListChanged(visReportCnts)
    }

    private fun addReportToCache(r: ReportData) {
        reports[r.cnt] = r
    }

    private fun observeReport(r: ReportData) {
        val stateObserver = Observer<ReportData.ReportState> { _ -> reportChanged(r)}
        val projectNameObserver = Observer<String> { _ -> reportChanged(r)}
        val projectExtraObserver = Observer<String> { _ -> reportChanged(r)}
        r.state.observeForever(stateObserver)
        r.project.name.observeForever(projectNameObserver)
        r.project.extra1.observeForever(projectExtraObserver)
    }

    private fun removeReportFromCache(cnt: Int) {
        // TODO: Don't know how remove the observer again
        //reports[id].state.removeObserver(observer)
        //reports.remove(id)
    }

    private fun clearReportCache() {
        reports.clear()
    }

    fun updateFilter() {
        fetchAllCntsFromDb()
    }

    private fun reportChanged(r: ReportData) {
        // If the new state is set to invisible and it is currently visible
        if (configuration().reportFilter.isFiltered(r) && visReportCnts.contains(r.cnt)) {
            synchronized(visReportCnts) {
                visReportCnts.remove(r.cnt)
            }
            for (o in reportListObservers)
                o.notifyReportRemoved(r.cnt)
        }
//TODO: We do not support changing visible from false to true, not a use case right now
    }

    fun getReportByCnt(cnt: Int, onLoaded:(r: ReportData)->Unit): ReportData {
        // If we have it in the cache, take if from there, else query the database and push it in the cache
        return reports[cnt]?.let {
            onLoaded(it)
            it
        } ?: run {
            val report = ReportData.createReport(cnt)
            addReportToCache(report)
            GlobalScope.launch(Dispatchers.Main) {
                val rDb = withContext(Dispatchers.IO) {
                    db.reportDao().getReportByCnt(cnt)
                }
                ReportData.getReportFromDb(rDb, report)
                observeReport(report)
                onLoaded(report)
            }
            return report
        }
    }

    fun getReport(): ReportData? {
        return activeReport
    }

    fun createNewReportAndSelect(): ReportData {
        val r = ReportData.createReport(configuration().currentId)
        configuration().activeReportId = r.cnt
        configuration().currentId += 1
        configuration().save()
        synchronized(visReportCnts) {
            visReportCnts.add(0, r.cnt)
        }
        activeReport = r
        addReportToCache(r)
        saveActiveReport()
        observeReport(r)
        for (o in reportListObservers)
            o.notifyReportAdded(r.cnt)
        return r
    }

    /* This is only for test purposes to create many reports. All are marked with MANY_REPORTS */
    /*
    fun duplicateReport(report: ReportData) {
        val jsonString = ReportData.getJsonFromReport(report)
        activeReport = ReportData.getReportFromJson(jsonString)
        activeReport.cnt = configuration().currentId
        reports.add(activeReport.id)
        configuration().activeReportId = activeReport.id
        configuration().currentId += 1
        configuration().save()
        saveActiveReportToFile(ArbeitsberichtApp.appContext)
    }*/

    fun deleteReport(cnt: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            db.reportDao().deleteByCnt(cnt)
        }
        synchronized(visReportCnts) {
            visReportCnts.remove(cnt)
        }
        //removeReportFromCache(cnt)
        for(o in reportListObservers)
            o.notifyReportRemoved(cnt)
    }

    fun selectReportByCnt(cnt: Int) {
        val report = getReportByCnt(cnt) {}
        activeReport = report
        configuration().activeReportId = cnt
        configuration().save()
    }

    private fun readStringFromFile(fileName: String, context: Context): String {
        var ret = ""
        try {
            val inputStream = context.openFileInput(fileName)

            if (inputStream != null) {
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var receiveString: String?
                val stringBuilder = StringBuilder()

                receiveString = bufferedReader.readLine()
                while (receiveString != null) {
                    stringBuilder.append(receiveString)
                    receiveString = bufferedReader.readLine()
                }

                inputStream.close()
                ret = stringBuilder.toString()
            }
        } catch (e: FileNotFoundException) {
            Log.e("login activity", "File not found: $e")
        } catch (e: IOException) {
            Log.e("login activity", "Can not read file: $e")
        }
        return ret
    }

    private fun readReportFromFile(fileName: String) : ReportData {
        val c: Context = ArbeitsberichtApp.appContext
        val jsonString = readStringFromFile(fileName, c)
        return ReportData.getReportFromJson(jsonString)
    }

    fun saveReport(r: ReportData, skipDictionaryUpdate: Boolean = false) {
        val reportDb = ReportDb.fromReport(r)
        r.lastStoreHash = reportDb.hashCode()
        val rDb = ReportDb.fromReport(r)
        GlobalScope.launch(Dispatchers.IO) {
            db.reportDao().update(rDb)
        }

        if (!skipDictionaryUpdate) {
            for (wi in r.workItemContainer.items) {
                addToWorkItemDictionary(wi.item.value!!)
            }
            for (m in r.materialContainer.items) {
                addToMaterialDictionary(m.item.value!!)
            }

            if (materialDictionaryChanged || workItemDictionaryChanged) {
                configuration().save()
                materialDictionaryChanged = false
                workItemDictionaryChanged = false
            }
        }
    }

    fun saveActiveReport() {
        activeReport?.let { saveReport(it) }
    }

    fun loadConfigurationFromFile(c: Context) {
        try {
            val fIn = c.openFileInput("configuration.json")
            val isr = InputStreamReader(fIn)
            Configuration.store = gson.fromJson(isr, ConfigurationStore::class.java)
            // Only temp until I rework the configuration data handling to copy to/from json/db similar as for reports
            Configuration.reportIdPattern.value = Configuration.store.reportIdPattern
            // TODO: Load filter from configuration
        }
        catch (e: Exception){
            Configuration.store = ConfigurationStore()
            configuration().save()
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

    fun updateLumpSums() {
        for(r in reports)
            r.value.lumpSumContainer.updateLumpSums()
    }
}

