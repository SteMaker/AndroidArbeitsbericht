package com.stemaker.arbeitsbericht

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.room.Room
import com.google.gson.Gson
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.data.ReportDatabase
import com.stemaker.arbeitsbericht.data.ReportDb
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.io.*
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "StorageHandler"

inline fun storageHandler() = StorageHandler

object StorageHandler {
    private val gson: Gson = Gson()
    private val reportIds: MutableList<String> = mutableListOf<String>()
    lateinit var activeReport: ReportData
    private val reports: MutableMap<String, ReportData> = mutableMapOf<String, ReportData>()

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
    ).build()

    private val inited = AtomicBoolean(false)
    var initJob: Job? = null

    private val accessMutex = Mutex()

    fun initialize(): Job? {
        if (inited.compareAndSet(false, true)) {
            initJob = GlobalScope.launch(Dispatchers.Main) {
                Log.d(TAG, "initialize start")
                val c: Context = ArbeitsberichtApp.appContext
                // Read the configuration. This needs to be low level -> no configuration() invocation yet
                loadConfigurationFromFile(c)

                if (Configuration.store.vers <= 118) {
                    withContext(Dispatchers.IO) {
                        // TODO: Temp
                        db.reportDao().deleteTable()
                    }
                    migrateToDatabase()
                    // This will update the version information and therefore there shouldn't be another migration
                    configuration()
                    deleteReportFilesAfterDbMigration()
                }

                withContext(Dispatchers.IO) {
                    reportIds.addAll(db.reportDao().getReportIds())
                }

                // Now we should be ready to do more
                if (configuration().activeReportId != "") {
                    if (reportIds.contains(configuration().activeReportId)) {
                        selectReportById(configuration().activeReportId)
                    } else {
                        configuration().activeReportId = ""
                    }
                }
                Log.d(TAG, "initialize end")
            }
        }
        return initJob
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
                    Log.d(TAG, "Migrating ${report.id} to database")
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

    suspend fun getListOfReports(): List<String> {
        accessMutex.lock()
        val ret = reportIds.toList()
        accessMutex.unlock()
        return ret
    }

    suspend fun getReportById(id: String): ReportData {
        // If we have it in the cache, take if from there, else query the database and push it in the cache
        accessMutex.lock()
        val ret = reports[id]?.let { it } ?: run {
            val rDb = withContext(Dispatchers.IO) {
                db.reportDao().getReportById(id)
            }
            val report = ReportData.getReportFromDb(rDb)
            reports[report.id] = report
            report
        }
        accessMutex.unlock()
        return ret
    }

    fun getReport(): ReportData {
        return activeReport
    }

    suspend fun createNewReportAndSelect(): ReportData {
        accessMutex.lock()
        configuration().lock()
        // TODO: make currentId atomic, use get and post inc here
        val rep = ReportData.createReport(configuration().currentId)
        configuration().activeReportId = rep.id
        configuration().currentId += 1
        configuration().save()
        configuration().unlock()
        reportIds.add(0, rep.id)
        activeReport = rep
        reports[rep.id] = rep
        withContext(Dispatchers.IO) {
            saveActiveReport()
        }
        accessMutex.unlock()
        return rep
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

    suspend fun selectReportById(id: String) {
        Log.d(TAG, "selectReportById start")
        val report = getReportById(id)
        activeReport = report
        configuration().lock()
        configuration().activeReportId = id
        configuration().save()
        configuration().unlock()
        Log.d(TAG, "selectReportById end")
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

    suspend fun saveReport(r: ReportData, skipDictionaryUpdate: Boolean = false) {
        val reportDb = ReportDb.fromReport(r)
        r.lastStoreHash = reportDb.hashCode()
        withContext(Dispatchers.IO) {
            val rDb = ReportDb.fromReport(r)
            val ret = db.reportDao().update(rDb)
        }

        configuration().lock()
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
        configuration().unlock()
    }

    suspend fun saveActiveReport() {
        saveReport(activeReport)
    }

    suspend fun renameReportsIfNeeded() {
        accessMutex.lock()
        Log.d(TAG, "renameReportsIfNeeded start")
        val oldIds = withContext(Dispatchers.IO) {
            db.reportDao().getReportIds()
        }
        for (old in oldIds) {
            val rDb = withContext(Dispatchers.IO) {
                db.reportDao().getReportById(old)
            }
            val updatedReport = ReportData.getReportFromDb(rDb)
            val rDb2 = ReportDb.fromReport(updatedReport)
            withContext(Dispatchers.IO) {
                db.reportDao().deleteById(old)
                db.reportDao().insert(rDb2)
            }
        }
        reportIds.clear()
        withContext(Dispatchers.IO) {
            reportIds.addAll(db.reportDao().getReportIds())
        }
        reports.clear()
        accessMutex.unlock()
    }

    suspend fun deleteReport(id: String) {
        accessMutex.lock()
        withContext(Dispatchers.IO) {
            Log.d(TAG, "deleteReport start")
            db.reportDao().deleteById(id)
            reportIds.remove(id)
            reports.remove(id)
            Log.d(TAG, "deleteReport end")
        }
        accessMutex.unlock()
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

