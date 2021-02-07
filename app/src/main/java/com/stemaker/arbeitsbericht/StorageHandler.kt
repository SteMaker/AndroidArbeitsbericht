
package com.stemaker.arbeitsbericht


import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import com.google.gson.Gson
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.data.ReportDataSerialized
import kotlinx.serialization.json.Json
import java.io.*

fun storageHandler(): StorageHandler {
    if(!StorageHandler.inited) {
        StorageHandler.initialize()
    }
    return StorageHandler
}

data class ReportReference(val id: String, val file: String) {
    override fun equals(other: Any?): Boolean {
        if(other is ReportReference) {
            if(other.id == id)
                return true
        }
        return false
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
class ReportReferences() {
    val reports = mutableListOf<ReportReference>()
    fun contains(id: String): Boolean {
        for(rep in reports) {
            if(rep.id == id) return true
        }
        return false
    }
    fun add(rep: ReportReference) {
        reports.add(rep)
    }
    fun remove(report: ReportData) {
        for(rep in reports) {
            if(rep.id == report.id) {
                reports.remove(rep)
                break
            }
        }
    }
    fun clear() {
        reports.clear()
    }
    val size: Int
        get() = reports.size
    fun get(idx: Int): ReportReference = reports[idx]
    fun sortByDescending() {
        reports.sortByDescending { it.id }
    }
    fun findFileById(id: String): String? {
        for(rep in reports) {
            if(rep.id == id)
                return rep.file
        }
        return null
    }
}

object StorageHandler {
    var inited: Boolean = false
    var gson = Gson()
    var reports = ReportReferences()
    lateinit var activeReport: ReportData

    private var _materialDictionary = mutableSetOf<String>()
    val materialDictionary: Set<String>
        get() = _materialDictionary
    private var materialDictionaryChanged: Boolean = false

    private var _workItemDictionary = mutableSetOf<String>()
    val workItemDictionary: Set<String>
        get() = _workItemDictionary
    private var workItemDictionaryChanged: Boolean = false

    fun initialize() {
        val c: Context = ArbeitsberichtApp.appContext
        if(!inited) {
            inited = true
            // Read the configuration. This needs to be low level -> no configuration() invocation yet
            Log.d("Arbeitsbericht.StorageHandler.myInit", "start")
            loadConfigurationFromFile(c)

            // Read the list of files that match report*.rpt
            createFolders()
            val appFiles = c.fileList()
            Log.d("Arbeitsbericht.StorageHandler.myInit", "Found ${appFiles.size} files")
            for (repFile in appFiles) {
                val exp = Regex("report(.*).rpt")
                if (exp.matches(repFile)) {
                    val repId = repFile.substring(repFile.lastIndexOf('/')+1).substringAfter("report").substringBefore(".rpt")
                    reports.add(ReportReference(repId, repFile))
                }
            }
            reports.sortByDescending()

            // Now we should be ready to do more
            if(configuration().activeReportId != "") {
                if (reports.contains(configuration().activeReportId)) {
                    selectReportById(configuration().activeReportId)
                } else {
                    configuration().activeReportId = ""
                }
            }

            Log.d("Arbeitsbericht.StorageHandler.myInit", "done")
        }
    }

    fun createFolders() {
        val folders = listOf<String>("inwork", "onhold", "done")
        for(folder in folders)
            File("${ArbeitsberichtApp.appContext.filesDir}/$folder").mkdirs()
    }

    // This must only be called by the configuration activity if the ID pattern was changed
    // Or on app start if naming scheme was changed due to an app update
    fun renameReportsIfNeeded() {
        val appFiles = ArbeitsberichtApp.appContext.fileList()
        reports.clear()
        for (repFile in appFiles) {
            val exp = Regex("report(.*).rpt")
            if (exp.matches(repFile)) {
                val rep: ReportData = readReportFromFile(repFile, ArbeitsberichtApp.appContext)
                val fileName1 = "${ArbeitsberichtApp.appContext.filesDir}/${repFile.substring(repFile.lastIndexOf('/')+1)}"
                val fileName2 = "${ArbeitsberichtApp.appContext.filesDir}/report${rep.id}.rpt"
                if (fileName1 != fileName2) {
                    val old = File(fileName1)
                    val new = File(fileName2)
                    val result = old.renameTo(new)
                    reports.add(ReportReference(rep.id, fileName2))
                }
            }
        }
        configuration().activeReportId = ""
    }

    fun getListOfReports(): ReportReferences {
        return reports
    }

    fun getReportByRef(report: ReportReference, c: Context): ReportData {
        return readReportFromFile(report.file, c)
    }

    fun getReport(): ReportData {
        return activeReport
    }

    fun createNewReportAndSelect() {
        val rep = ReportData.createReport(configuration().currentId)
        Log.d("Arbeitsbericht.StorageHandler.createNewReportAndSelect", "Created new report with ID ${rep.id}")
        reports.add(ReportReference(rep.id, reportToReportFile(rep)))
        activeReport = rep
        configuration().activeReportId = rep.id
        configuration().currentId += 1
        configuration().save()
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

    fun selectReportById(id: String, c: Context = ArbeitsberichtApp.appContext) {
        Log.d("Arbeitsbericht.StorageHandler.selectReportById", c.toString())
        configuration().activeReportId = id
        activeReport = readReportFromFile(reports.findFileById(id)!!, c)
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

    private fun writeStringToFile(fileName: String, data: String, context: Context) {
        try {
            val outputStreamWriter = OutputStreamWriter(context.openFileOutput(fileName, Context.MODE_PRIVATE))
            outputStreamWriter.write(data)
            outputStreamWriter.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
    }

    private fun readReportFromFile(fileName: String, c: Context) : ReportData {
        Log.d("Arbeitsbericht.StorageHandler.readReportFromFile", "Trying to read from file $fileName")
        val jsonString = readStringFromFile(fileName, c)
        return ReportData.getReportFromJson(jsonString)
    }

    fun saveReportToFile(c: Context, report: ReportData, skipDictionaryUpdate: Boolean = false) {
        val jsonString = ReportData.getJsonFromReport(report)
        report.lastStoreHash = jsonString.hashCode()
        val newFileName = reportToReportFile(report)
        val oldFileName = reports.findFileById(report.id)
        if(oldFileName != "" && newFileName != oldFileName) {
            // Seems the state changed so the folder changed, move it
            File(oldFileName).renameTo(File(newFileName))
        }
        writeStringToFile(newFileName, jsonString, c)
        Log.d("Arbeitsbericht.StorageHandler.saveReportToFile", "Saved to file $newFileName")

        if(!skipDictionaryUpdate) {
            for (wi in report.workItemContainer.items) {
                addToWorkItemDictionary(wi.item.value!!)
            }
            for (m in report.materialContainer.items) {
                addToMaterialDictionary(m.item.value!!)
            }

            if (materialDictionaryChanged || workItemDictionaryChanged) {
                configuration().save()
                materialDictionaryChanged = false
                workItemDictionaryChanged = false
            }
        }
    }

    fun saveActiveReportToFile(c: Context) {
        saveReportToFile(c, activeReport)
    }

    private fun reportToReportFile(report: ReportData): String {
        val stateDir = ReportData.ReportState.toDirName(report.state.value!!)
        if(stateDir != "")
            return "${ArbeitsberichtApp.appContext.filesDir}/${stateDir}/report${report.id}.rpt"
        else
            return "${ArbeitsberichtApp.appContext.filesDir}/report${report.id}.rpt"
    }

    fun deleteReport(report: ReportData, c: Context) {
        Log.d("Arbeitsbericht.StorageHandler.deleteReport", "Deleting report with ID ${report.id}")
        c.deleteFile(reportToReportFile(report))
        reports.remove(report)
    }

    fun loadConfigurationFromFile(c: Context) {
        Log.d("Arbeitsbericht.StorageHandler.loadConfigurationFromFile", "")
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
        Log.d("Arbeitsbericht.StorageHandler.saveConfigurationToFile", "currentId = ${configuration().currentId}; num lump sums = ${configuration().lumpSums.size}")
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

