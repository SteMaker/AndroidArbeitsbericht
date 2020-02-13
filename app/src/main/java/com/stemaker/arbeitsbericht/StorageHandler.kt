
package com.stemaker.arbeitsbericht


import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import com.google.gson.Gson
import com.stemaker.arbeitsbericht.data.configuration.Configuration
import com.stemaker.arbeitsbericht.data.configuration.ConfigurationStore
import com.stemaker.arbeitsbericht.data.configuration.configuration
import com.stemaker.arbeitsbericht.data.hoursreport.HoursReportData
import com.stemaker.arbeitsbericht.data.workreport.ReportData
import java.io.*

private const val TAG = "StorageHandler"

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
    var hoursReports = mutableListOf<Long>()
    lateinit var activeReport: ReportData
    lateinit var activeHoursReport: HoursReportData

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
            Log.d(TAG, "initialize")
            loadConfigurationFromFile(c)

            // Read the list of files that match report*.rpt or hoursreport*.rpt
            val appFiles = c.fileList()
            Log.d(TAG, "Found ${appFiles.size} files")
            for (repFile in appFiles) {
                val exp = Regex("report(.*).rpt")
                val expHours = Regex("hoursreport(.*).rpt")
                if (exp.matches(repFile)) {
                    val repId = repFile.substring(repFile.lastIndexOf('/') + 1).substringAfter("report").substringBefore(".rpt")
                    reports.add(repId)
                } else if(expHours.matches(repFile)) {
                    val repId = repFile.substring(repFile.lastIndexOf('/') + 1).substringAfter("hoursreport").substringBefore(".rpt")
                    hoursReports.add(repId.toLong())
                } else {
                    Log.d(TAG, "Found file $repFile not matching the expected pattern")
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
            if(configuration().activeHoursReportId != 0.toLong()) {
                if (hoursReports.contains(configuration().activeHoursReportId)) {
                    selectHoursReportById(configuration().activeHoursReportId)
                } else {
                    configuration().activeHoursReportId = 0
                }
            }

            Log.d(TAG, "initialize done")
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

    fun getListOfHoursReports(): MutableList<Long> {
        return hoursReports
    }

    fun getHoursReportById(reportId: Long, c: Context): HoursReportData {
        return readHoursReportFromFile(hoursReportIdToHoursReportFile(reportId), c)
    }

    fun getHoursReport(): HoursReportData {
        return activeHoursReport
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

    fun createNewHoursReportAndSelect() {
        val rep = HoursReportData.createReport()
        Log.d(TAG, "Created new hours report with ID ${rep.id}")
        hoursReports.add(rep.id)
        activeHoursReport = rep
        configuration().activeHoursReportId = rep.id
        configuration().save()
    }

    fun selectReportById(id: String, c: Context = ArbeitsberichtApp.appContext) {
        configuration().activeReportId = id
        activeReport = readReportFromFile(reportIdToReportFile(id), c)
        configuration().save()
    }

    fun selectHoursReportById(id: Long, c: Context = ArbeitsberichtApp.appContext) {
        configuration().activeHoursReportId = id
        activeHoursReport = readHoursReportFromFile(hoursReportIdToHoursReportFile(id), c)
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
            Log.e(TAG, "readStringFromFile: File not found: $e")
        } catch (e: IOException) {
            Log.e(TAG, "readStringFromFile: Can not read file: $e")
        }
        return ret
    }

    private fun writeStringToFile(fileName: String, data: String, context: Context) {
        try {
            val outputStreamWriter = OutputStreamWriter(context.openFileOutput(fileName, Context.MODE_PRIVATE))
            outputStreamWriter.write(data)
            outputStreamWriter.close()
        } catch (e: IOException) {
            Log.e(TAG, "Exception: File write failed: $e")
        }
    }

    private fun readReportFromFile(fileName: String, c: Context) : ReportData {
        Log.d("Arbeitsbericht.StorageHandler.readReportFromFile", "Trying to read from file $fileName")
        val jsonString = readStringFromFile(fileName, c)
        return ReportData.getReportFromJson(jsonString)
    }

    private fun readHoursReportFromFile(fileName: String, c: Context) : HoursReportData {
        Log.d(TAG, "Trying to read from file $fileName")
        val jsonString = readStringFromFile(fileName, c)
        return HoursReportData.getReportFromJson(jsonString)
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

    fun saveActiveHoursReportToFile(c: Context) {
        val jsonString = HoursReportData.getJsonFromReport(activeHoursReport)
        activeHoursReport.lastStoreHash = jsonString.hashCode()
        val fileName = hoursReportIdToHoursReportFile(activeHoursReport.id)
        writeStringToFile(fileName, jsonString, c)
        Log.d(TAG, "Saved to file $fileName")

        for(wi in activeHoursReport.workItems) {
            addToWorkItemDictionary(wi.value!!)
        }

        if(workItemDictionaryChanged) {
            configuration().save()
            workItemDictionaryChanged = false
        }
    }

    fun reportIdToReportFile(id: String): String {
        return "report${id}.rpt"
    }

    fun hoursReportIdToHoursReportFile(id: Long): String {
        return "hoursreport${id}.rpt"
    }

    fun deleteReport(id: String, c: Context) {
        Log.d(TAG, "Deleting work report with ID $id")
        c.deleteFile(reportIdToReportFile(id))
        reports.remove(id)
    }

    fun deleteHoursReport(id: Long, c: Context) {
        Log.d(TAG, "Deleting hours report with ID $id")
        c.deleteFile(hoursReportIdToHoursReportFile(id))
        hoursReports.remove(id)
    }

    fun loadConfigurationFromFile(c: Context) {
        Log.d("Arbeitsbericht.StorageHandler.loadConfigurationFromFile", "")
        try {
            val fIn = c.openFileInput("configuration.json")
            val isr = InputStreamReader(fIn)
            Configuration.store = gson.fromJson(isr, ConfigurationStore::class.java)
        }
        catch (e: FileNotFoundException){
            Configuration.store =
                ConfigurationStore()
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

