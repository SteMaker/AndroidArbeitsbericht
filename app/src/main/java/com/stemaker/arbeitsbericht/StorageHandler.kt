
package com.stemaker.arbeitsbericht


import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import com.google.gson.Gson
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.helpers.showInfoDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
    var reports = mutableListOf<Int>()
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
        if(inited == false) {
            inited = true
            // Read the configuration
            Log.d("Arbeitsbericht.StorageHandler.myInit", "start")
            loadConfigurationFromFile(c)
            Log.d("Arbeitsbericht.StorageHandler.myInit", "Next free ID is ${configuration().currentId}")

            // Read the list of files that match report$id.json
            var maxId: Int = 0
            val appFiles = c.fileList()
            Log.d("Arbeitsbericht.StorageHandler.myInit", "Found ${appFiles.size} files")
            for (repFile in appFiles) {
                val exp = Regex("report[0-9]{8}.rpt")
                if (exp.matches(repFile)) {
                    Log.d("Arbeitsbericht.StorageHandler.myInit", "Found file $repFile matching the expected pattern")
                    val rep: ReportData = readReportFromFile(repFile, c)
                    reports.add(rep.id.value!!)
                    if (rep.id.value!! > maxId)
                        maxId = rep.id.value!!
                } else {
                    Log.d("Arbeitsbericht.StorageHandler.myInit", "Found file $repFile not matching the expected pattern")
                }
            }

            // Consistency check if the report IDs are beyond the next ID
            if(maxId >= configuration().currentId) {
                Log.w("Arbeitsbericht.StorageHandler.myInit", "A report has an ID which is higher than the next one to use")
                GlobalScope.launch(Dispatchers.Main) {
                    showInfoDialog(c.getString(R.string.report_id_mismatch), c, c.getString(R.string.report_id_mismatch_detail))
                }
            }
            if(configuration().activeReportId > 0)
                selectReportById(configuration().activeReportId)

            Log.d("Arbeitsbericht.StorageHandler.myInit", "done")
        }
    }

    fun getListOfReports(): MutableList<Int> {
        return reports
    }

    fun getReportById(reportId: Int, c: Context): ReportData {
        return readReportFromFile("report${reportId.toString().padStart(8, '0')}.rpt", c)
    }

    fun getReport(): ReportData {
        return activeReport
    }

    fun createNewReportAndSelect(c: Context) {
        val rep = ReportData.createReport(configuration().currentId)
        Log.d("Arbeitsbericht.StorageHandler.createNewReportAndSelect", "Created new report with ID ${rep.id.value!!}")
        reports.add(rep.id.value!!)
        activeReport = rep
        configuration().activeReportId = rep.id.value!!
        configuration().currentId += 1
        configuration().save()
    }

    fun selectReportById(id: Int, c: Context = ArbeitsberichtApp.appContext) {
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
        val rep = ReportData.getReportFromJson(jsonString)
        return rep
    }

    fun saveActiveReportToFile(c: Context) {
        val jsonString = ReportData.getJsonFromReport(activeReport)
        activeReport.lastStoreHash = jsonString.hashCode()
        val fileName = reportIdToReportFile(activeReport.id.value!!)
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

    fun reportIdToReportFile(id: Int): String {
        return "report${id.toString().padStart(8,'0')}.rpt"
    }

    fun deleteReport(id: Int, c: Context) {
        Log.d("Arbeitsbericht.StorageHandler.deleteReport", "Deleting report with ID $id")
        c.deleteFile(reportIdToReportFile(id))
        reports.remove(id)
    }

    fun loadConfigurationFromFile(c: Context) {
        Log.d("Arbeitsbericht.StorageHandler.loadConfigurationFromFile", "")
        try {
            val fIn = c.openFileInput("configuration.json")
            val isr = InputStreamReader(fIn)
            configuration().store = gson.fromJson(isr, ConfigurationStore::class.java)
            Log.d("Arbeitsbericht.StorageHandler.loadConfigurationFromFile", "Next ID to be used: ${configuration().currentId}")
        }
        catch (e: FileNotFoundException){
            Log.d("Arbeitsbericht.StorageHandler.loadConfigurationFromFile", "No configuration file found, creating a new one")
            configuration().store = ConfigurationStore()
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

