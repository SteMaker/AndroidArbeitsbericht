
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

object StorageHandler {
    var inited: Boolean = false
    var gson = Gson()
    var reports = mutableListOf<String>()
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
            val appFiles = c.fileList()
            Log.d("Arbeitsbericht.StorageHandler.myInit", "Found ${appFiles.size} files")
            for (repFile in appFiles) {
                val exp = Regex("report(.*).rpt")
                if (exp.matches(repFile)) {
                    Log.d("Arbeitsbericht.StorageHandler.myInit", "Found file $repFile matching the expected pattern")
                    val repId = repFile.substring(repFile.lastIndexOf('/')+1).substringAfter("report").substringBefore(".rpt")
                    reports.add(repId)
                } else {
                    Log.d("Arbeitsbericht.StorageHandler.myInit", "Found file $repFile not matching the expected pattern")
                }
            }

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
                    reports.add(rep.id)
                }
            }
        }
        configuration().activeReportId = ""
    }

    fun getListOfReports(): MutableList<String> {
        return reports
    }

    fun getReportById(reportId: String, c: Context): ReportData {
        return readReportFromFile(reportIdToReportFile(reportId), c)
    }

    fun getReport(): ReportData {
        return activeReport
    }

    fun createNewReportAndSelect() {
        val rep = ReportData.createReport(configuration().currentId)
        Log.d("Arbeitsbericht.StorageHandler.createNewReportAndSelect", "Created new report with ID ${rep.id}")
        reports.add(rep.id)
        activeReport = rep
        configuration().activeReportId = rep.id
        configuration().currentId += 1
        configuration().save()
    }

    fun selectReportById(id: String, c: Context = ArbeitsberichtApp.appContext) {
        Log.d("Arbeitsbericht.StorageHandler.selectReportById", c.toString())
        configuration().activeReportId = id
        activeReport = readReportFromFile(reportIdToReportFile(id), c)
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

    fun saveActiveReportToFile(c: Context) {
        val jsonString = ReportData.getJsonFromReport(activeReport)
        activeReport.lastStoreHash = jsonString.hashCode()
        val fileName = reportIdToReportFile(activeReport.id)
        writeStringToFile(fileName, jsonString, c)
        Log.d("Arbeitsbericht.StorageHandler.saveReportToFile", "Saved to file $fileName")

        for(wi in activeReport.workItemContainer.items) {
            addToWorkItemDictionary(wi.item.value!!)
        }
        for(m in activeReport.materialContainer.items) {
            addToMaterialDictionary(m.item.value!!)
        }

        if(materialDictionaryChanged || workItemDictionaryChanged) {
            configuration().save()
            materialDictionaryChanged = false
            workItemDictionaryChanged = false
        }
    }

    fun reportIdToReportFile(id: String): String {
        return "report${id}.rpt"
    }

    fun deleteReport(id: String, c: Context) {
        Log.d("Arbeitsbericht.StorageHandler.deleteReport", "Deleting report with ID $id")
        c.deleteFile(reportIdToReportFile(id))
        reports.remove(id)
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

