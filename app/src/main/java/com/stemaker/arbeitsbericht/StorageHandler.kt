package com.stemaker.arbeitsbericht

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.lifecycle.Observer
import androidx.room.Room
import com.google.gson.Gson
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.data.ReportDatabase
import com.stemaker.arbeitsbericht.data.ReportDb
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.*
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

    private val db = Room.databaseBuilder(
        ArbeitsberichtApp.appContext,
        ReportDatabase::class.java, "Arbeitsbericht-Reports"
    ).fallbackToDestructiveMigration().build()

    private val inited = AtomicBoolean(false)
    var initJob: Job? = null
    val mutex = Mutex()

    var reportListObservers = mutableListOf<ReportListObserver>()

    // TODO: Store the filter in the configuration
    private val stateFilter = mutableSetOf<Int>(ReportData.ReportState.toInt(ReportData.ReportState.IN_WORK),
        ReportData.ReportState.toInt(ReportData.ReportState.ON_HOLD),
        ReportData.ReportState.toInt(ReportData.ReportState.DONE))

    fun initialize(): Job? {
        if (inited.compareAndSet(false, true)) {
            initJob = GlobalScope.launch(Dispatchers.Main) {
                Log.d(TAG, "initialize start")
                val c: Context = ArbeitsberichtApp.appContext
                // Read the configuration. This needs to be low level -> no configuration() invocation yet
                loadConfigurationFromFile(c)

                if (Configuration.store.vers <= 118) {
                    withContext(Dispatchers.IO) {
                        // TODO: Temp, add check and stop with notification that data might get lost
                        db.reportDao().deleteTable()
                    }
                    migrateToDatabase()
                    // This will update the version information and therefore there shouldn't be another migration
                    configuration()
                    configuration().activeReportId = -1
                    deleteReportFilesAfterDbMigration()
                }

                val cnts = withContext(Dispatchers.IO) {
                    db.reportDao().getStateFilteredReportIds(stateFilter)
                }
                visReportCnts.addAll(cnts)

                // Now we should be ready to do more
                if (configuration().activeReportId != -1) {
                    if (visReportCnts.contains(configuration().activeReportId)) {
                        selectReportByCnt(configuration().activeReportId)
                    } else {
                        configuration().activeReportId = -1
                    }
                }
                Log.d(TAG, "initialize end")
            }
        }
        // Activities need to wait for this initJob before they are allowed to access the storageHandler()
        return initJob
    }

    private fun fetchAllCntsFromDb() {
        val localStateFilter = mutableSetOf<Int>()
        localStateFilter.addAll(stateFilter)
        GlobalScope.launch(Dispatchers.Main) {
            val cnts = withContext(Dispatchers.IO) {
                db.reportDao().getStateFilteredReportIds(localStateFilter)
            }
            synchronized(visReportCnts) {
                visReportCnts.clear()
                visReportCnts.addAll(cnts)
            }
            for (o in reportListObservers)
                o.notifyReportListChanged(visReportCnts)
        }
    }

    private suspend fun migrateToDatabase() {
        val c: Context = ArbeitsberichtApp.appContext
        val appFiles = c.fileList()
        val reportDao = db.reportDao()

        for (repFile in appFiles) {
            val exp = Regex("report(.*).rpt")
            if (exp.matches(repFile)) {
                val report = readReportFromFile(repFile)
                val r = ReportDb.fromReport(report)
                withContext(Dispatchers.IO) {
                    reportDao.insert(r)
                }
            }
        }
    }

    private fun deleteReportFilesAfterDbMigration() {
        val c: Context = ArbeitsberichtApp.appContext
        val appFiles = c.fileList()
        for (repFile in appFiles) {
            val exp = Regex("report(.*).rpt")
            if (exp.matches(repFile)) {
                val filename = "${ArbeitsberichtApp.appContext.filesDir}/$repFile"
                File(filename).delete()
            }
        }
    }

    fun addReportListObserver(obs: ReportListObserver) {
        reportListObservers.add(obs)
        obs.notifyReportListChanged(visReportCnts)
    }

    private fun addReportToCache(r: ReportData) {
        reports[r.cnt] = r
        val observer = Observer<ReportData.ReportState> { _ -> reportStateChanged(r)}
        r.state.observeForever(observer)
    }

    private fun removeReportFromCache(cnt: Int) {
        // TODO: Don't know how remove the observer again
        //reports[id].state.removeObserver(observer)
        //reports.remove(id)
    }

    private fun clearReportCache() {
        reports.clear()
    }

    fun setStateFilter(f: Set<Int>) {
        if(f != stateFilter) {
            stateFilter.clear()
            stateFilter.addAll(f)
            fetchAllCntsFromDb()
        }
    }

    private fun reportStateChanged(r: ReportData) {
        // If the new state is set to invisible and it is currently visible
        if (!stateFilter.contains(ReportData.ReportState.toInt(r.state.value!!)) && visReportCnts.contains(r.id)) {
            synchronized(visReportCnts) {
                visReportCnts.remove(r.id)
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
            GlobalScope.launch(Dispatchers.IO) {
                mutex.withLock {
                    addReportToCache(report)
                    val rDb = db.reportDao().getReportByCnt(cnt)
                    ReportData.getReportFromDb(rDb, report)
                    onLoaded(report)
                }
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

    private fun loadConfigurationFromFile(c: Context) {
        Log.d(TAG, "")
        try {
            val fIn = c.openFileInput("configuration.json")
            val isr = InputStreamReader(fIn)
            Configuration.store = gson.fromJson(isr, ConfigurationStore::class.java)
        }
        catch (e: FileNotFoundException){
            Configuration.store = ConfigurationStore()
            configuration().save()
        }
    }

    fun saveConfigurationToFile(c: Context) {
        configuration().materialDictionary = materialDictionary
        configuration().workItemDictionary = workItemDictionary
        val fOut = c.openFileOutput("configuration.json", MODE_PRIVATE)
        Log.d("TAG", "currentId = ${configuration().currentId}; num lump sums = ${configuration().lumpSums.size}")
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
}

