package com.stemaker.arbeitsbericht


import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.stemaker.arbeitsbericht.data.ReportData
import java.io.*

fun storageHandler(): StorageHandler {
    if(!StorageHandler.inited) {
        StorageHandler.myInit()
    }
    return StorageHandler
}

object StorageHandler {
    var inited: Boolean = false
    var gson = Gson()
    var reports = mutableListOf<Int>()
    lateinit var activeReport: ReportData
    lateinit var configuration: Configuration
    lateinit var materialDictionary: MaterialDictionary
    var materialDictionaryChanged: Boolean = false
    lateinit var workItemDictionary: WorkItemDictionary
    var workItemDictionaryChanged: Boolean = false

    fun myInit() {
        val c: Context = ArbeitsberichtApp.appContext
        if(inited == false) {
            inited = true
            // Read the configuration
            Log.d("Arbeitsbericht.StorageHandler.myInit", "start")
            loadConfigurationFromFile(c)
            Log.d("Arbeitsbericht.StorageHandler.myInit", "Next free ID is ${configuration.currentId}")

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
            if(maxId >= configuration.currentId) {
                Log.w("Arbeitsbericht.StorageHandler.myInit", "A report has an ID which is higher than the next one to use")
                val toast = Toast.makeText(c, "Fehler bei der aktuellen laufenden Nummer", Toast.LENGTH_LONG)
                toast.show()
            }

            // Read the list of material items
            readMaterialDictionaryFromFile(c)
            // Read the list of work items
            readWorkItemDictionaryFromFile(c)

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
        val rep = ReportData.createReport(configuration.currentId)
        Log.d("Arbeitsbericht.StorageHandler.createNewReportAndSelect", "Created new report with ID ${rep.id.value!!}")
        reports.add(rep.id.value!!)
        activeReport = rep
        configuration.currentId += 1
        saveConfigurationToFile(c)
    }

    fun selectReportById(id: Int, c: Context = ArbeitsberichtApp.appContext) {
        Log.d("Arbeitsbericht.StorageHandler.selectReportById", c.toString())
        activeReport = readReportFromFile(reportIdToReportFile(id), c)
    }

    private fun readStringFromFile(fileName: String, context: Context): String {
        var ret = ""
        try {
            val inputStream = context.openFileInput(fileName)

            if (inputStream != null) {
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var receiveString: String? = ""
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
        /*
        if(reportSer.lump_sums == null) {
            Log.d("Arbeitsbericht.StorageHandler.readReportFromFile", "Report seems to be old, not having a lump sum, adding it")
            reportSer.lump_sums = mutableListOf<LumpSum>()
        }
        if(reportSer.photos == null) {
            Log.d("Arbeitsbericht.StorageHandler.readReportFromFile", "Report seems to be old, not having photos, adding it")
            reportSer.photos = mutableListOf<Photo>()
        }
        */
        return rep
    }

    fun saveReportToFile(r: ReportData, c: Context) {
        r.updateLastChangeDate()
        val jsonString = ReportData.getJsonFromReport(r)
        val fileName = reportIdToReportFile(r.id.value!!)
        writeStringToFile(fileName, jsonString, c)
        Log.d("Arbeitsbericht.StorageHandler.saveReportToFile", "Saved to file $fileName")

        for(wi in r.workItemContainer.items) {
            addToWorkItemDictionary(wi.item.value!!)
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
            configuration = gson.fromJson(isr, Configuration::class.java)
            if(configuration.lumpSums == null)
                configuration.lumpSums = mutableListOf<String>()
            Log.d("Arbeitsbericht.StorageHandler.loadConfigurationFromFile", "Next ID to be used: ${configuration.currentId}")
        }
        catch (e: FileNotFoundException){
            Log.d("Arbeitsbericht.StorageHandler.loadConfigurationFromFile", "No configuration file found, creating a new one")
            configuration = Configuration()
            saveConfigurationToFile(c)
        }
    }

    fun saveConfigurationToFile(c: Context) {
        val fOut = c.openFileOutput("configuration.json", MODE_PRIVATE)
        Log.d("Arbeitsbericht.StorageHandler.saveConfigurationToFile", "currentId = ${configuration.currentId}; num lump sums = ${configuration.lumpSums.size}")
        val osw = OutputStreamWriter(fOut)
        gson.toJson(configuration, osw)
        osw.close()
    }

    private fun readMaterialDictionaryFromFile(c: Context) {
        Log.d("Arbeitsbericht.StorageHandler.readMaterialDictionaryFromFile", "")
        if(materialDictionaryChanged) {
            Log.w("Arbeitsbericht.StorageHandler.readMaterialDictionaryFromFile", "Unexpected that we have a pending material dictionary update while we want to read it")
            saveMaterialDictionaryToFile(c)
        }
        try {
            val fIn = c.openFileInput("material_dictionary.json")
            val isr = InputStreamReader(fIn)
            materialDictionary = gson.fromJson(isr, MaterialDictionary::class.java)
        }
        catch (e: FileNotFoundException){
            Log.d("Arbeitsbericht.StorageHandler.readMaterialDictionary", "No material dictionary file found, creating a new one")
            materialDictionary = MaterialDictionary()
            saveMaterialDictionaryToFile(c)
        }
    }

    private fun saveMaterialDictionaryToFile(c: Context) {
        if(materialDictionaryChanged) {
            Log.d("Arbeitsbericht.StorageHandler.saveMaterialDictionaryToFile", "saving")
            val fOut = c.openFileOutput("material_dictionary.json", MODE_PRIVATE)
            val osw = OutputStreamWriter(fOut)
            gson.toJson(materialDictionary, osw)
            osw.close()
            materialDictionaryChanged = false
        }
    }

    fun addToMaterialDictionary(item: String) {
        if(materialDictionary.items.add(item)) {
            materialDictionaryChanged = true
        }
    }

    private fun readWorkItemDictionaryFromFile(c: Context) {
        Log.d("Arbeitsbericht.StorageHandler.readWorkItemDictionaryFromFile", "")
        if(workItemDictionaryChanged) {
            Log.w("Arbeitsbericht.StorageHandler.readWorkItemDictionaryFromFile", "Unexpected that we have a pending work item dictionary update while we want to read it")
            saveWorkItemDictionaryToFile(c)
        }
        try {
            val fIn = c.openFileInput("workitem_dictionary.json")
            val isr = InputStreamReader(fIn)
            workItemDictionary = gson.fromJson(isr, WorkItemDictionary::class.java)
        }
        catch (e: FileNotFoundException){
            Log.d("Arbeitsbericht.StorageHandler.readWorkItemDictionary", "No workItem dictionary file found, creating a new one")
            workItemDictionary = WorkItemDictionary()
            saveWorkItemDictionaryToFile(c)
        }
    }

    private fun saveWorkItemDictionaryToFile(c: Context) {
        if(workItemDictionaryChanged) {
            Log.d("Arbeitsbericht.StorageHandler.saveWorkItemDictionaryToFile", "saving")
            val fOut = c.openFileOutput("workitem_dictionary.json", MODE_PRIVATE)
            val osw = OutputStreamWriter(fOut)
            gson.toJson(workItemDictionary, osw)
            osw.close()
            workItemDictionaryChanged = false
        }
    }

    private fun addToWorkItemDictionary(item: String) {
        if(workItemDictionary.items.add(item)) {
            workItemDictionaryChanged = true
        }
    }
}

