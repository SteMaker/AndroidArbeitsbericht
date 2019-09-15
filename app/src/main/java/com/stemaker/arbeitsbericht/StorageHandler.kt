package com.stemaker.arbeitsbericht


import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.*
import java.util.*

object StorageHandler {
    var inited = 0
    var gson = Gson()
    var reports = mutableListOf<Int>()
    lateinit var activeReport: Report
    lateinit var configuration: Configuration
    lateinit var materialDictionary: MaterialDictionary
    var materialDictionaryChanged: Boolean = false
    lateinit var workItemDictionary: WorkItemDictionary
    var workItemDictionaryChanged: Boolean = false

    fun myInit(c: Context) {
        if(inited == 0) {
            // Read the configuration
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
                    val rep: Report = readReportFromFile(repFile, c)
                    reports.add(rep.id)
                    if (rep.id > maxId)
                        maxId = rep.id
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

            inited = 1
        }
    }

    fun getListOfReports(): MutableList<Int> {
        return reports
    }

    fun getReportById(reportId: Int, c: Context): Report {
        return readReportFromFile("report${reportId.toString().padStart(8, '0')}.rpt", c)
    }

    fun getReport(): Report {
        return activeReport
    }

    fun createNewReportAndSelect(c: Context) {
        val rep = Report(configuration.currentId)
        reports.add(rep.id)
        activeReport = rep
        configuration.currentId += 1
        saveConfigurationToFile(c)
    }

    fun selectReportById(id: Int, c: Context) {
        activeReport = readReportFromFile(reportIdToReportFile(id), c)
    }

    fun readReportFromFile(filename: String, c: Context) : Report {
        Log.d("Arbeitsbericht.StorageHandler.readReportFromFile", "Trying to read from file $filename")
        val fIn = c.openFileInput(filename)
        val isr = InputStreamReader(fIn)
        val report = gson.fromJson(isr, Report::class.java)
        if(report.lump_sums == null) {
            Log.d("Arbeitsbericht.StorageHandler.readReportFromFile", "Report seems to be old, not having a lump sum, adding it")
            report.lump_sums = mutableListOf<LumpSum>()
        }
        return report
    }

    fun saveReportToFile(r: Report, c: Context) {
        r.updateLastChangeDate()
        val fn = reportIdToReportFile(r.id)
        val fOut = c.openFileOutput(fn, MODE_PRIVATE)
        Log.d("Arbeitsbericht.StorageHandler.saveReportToFile", "Saved to file $fn")
        val osw = OutputStreamWriter(fOut)
        gson.toJson(r, osw)
        osw.close()
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

    fun readMaterialDictionaryFromFile(c: Context) {
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

    fun saveMaterialDictionaryToFile(c: Context) {
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

    fun readWorkItemDictionaryFromFile(c: Context) {
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

    fun saveWorkItemDictionaryToFile(c: Context) {
        if(workItemDictionaryChanged) {
            Log.d("Arbeitsbericht.StorageHandler.saveWorkItemDictionaryToFile", "saving")
            val fOut = c.openFileOutput("workitem_dictionary.json", MODE_PRIVATE)
            val osw = OutputStreamWriter(fOut)
            gson.toJson(workItemDictionary, osw)
            osw.close()
            workItemDictionaryChanged = false
        }
    }

    fun addToWorkItemDictionary(item: String) {
        if(workItemDictionary.items.add(item)) {
            workItemDictionaryChanged = true
        }
    }
}

