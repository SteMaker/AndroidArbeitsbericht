package com.stemaker.arbeitsbericht.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stemaker.arbeitsbericht.configuration
import kotlinx.serialization.json.Json
import java.util.*

private const val TAG="ReportData"

class ReportData private constructor(var cnt: Int = 0): ViewModel() {
    private var _id = MutableLiveData<String>()
    val idLive: LiveData<String>
        get() { return MutableLiveData<String>().apply { value = id } }

    val id: String
        get() {
            var string = configuration().reportIdPattern
            // Replace %<n>c -> running counter
            val regex = """(%c|%[0-9]c)""".toRegex()
            string = regex.replace(string) { m ->
                Log.d(TAG, m.value)
                when (m.value) {
                    "%c" -> cnt.toString()
                    else -> {
                        val length = m.value.substring(1,2).toInt()
                        cnt.toString().padStart(length, '0')
                    }
                }
            }

            // Replace %y, %m, %d -> Date
            string = string.replace("%y", create_date.value!!.substring(6, 10))
            string = string.replace("%m", create_date.value!!.substring(3, 5))
            string = string.replace("%d", create_date.value!!.substring(0, 2))

            // Replace others
            string = string.replace("%e", configuration().employeeName)
            string = string.replace("%p", configuration().deviceName)

            return string
        }

    private val _create_date = MutableLiveData<String>()
    val create_date: LiveData<String>
        get() = _create_date

    var lastStoreHash: Int = 0

    var project = ProjectData()
    var bill = BillData()
    val workTimeContainer = WorkTimeContainerData()
    val workItemContainer = WorkItemContainerData()
    val materialContainer = MaterialContainerData()
    val lumpSumContainer = LumpSumContainerData()
    val photoContainer = PhotoContainerData()
    val signatureData = SignatureData()

    init {
        _create_date.value = getCurrentDate()
    }

    private fun copyFromSerialized(r: ReportDataSerialized) {
        cnt = r.id
        _create_date.value = r.create_date
        project.copyFromSerialized(r.project)
        bill.copyFromSerialized(r.bill)
        workTimeContainer.copyFromSerialized(r.workTimeContainer)
        workItemContainer.copyFromSerialized(r.workItemContainer)
        materialContainer.copyFromSerialized(r.materialContainer)
        lumpSumContainer.copyFromSerialized(r.lumpSumContainer)
        photoContainer.copyFromSerialized(r.photoContainer)
        signatureData.copyFromSerialized(r.signatureData)
    }

    private fun getCurrentDate(): String {
        val d = Date()
        val cal = Calendar.getInstance()
        cal.time = d
        return cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2,'0') + "." +
                (cal.get(Calendar.MONTH)+1).toString().padStart(2,'0') + "." +
                cal.get(Calendar.YEAR).toString().padStart(4,'0')
    }

    companion object {
        fun getReportFromJson(jsonData: String): ReportData {
            val serialized = Json.parse(ReportDataSerialized.serializer(), jsonData)
            val report = ReportData()
            report.copyFromSerialized(serialized)
            return report
        }

        fun getJsonFromReport(r: ReportData): String {
            val serialized = ReportDataSerialized()
            serialized.copyFromData(r) // Copy from data object into serialized object
            val json: String = Json.stringify(ReportDataSerialized.serializer(), serialized)
            return json
        }

        fun createReport(idNr: Int): ReportData {
            val d = Date()
            val cal = Calendar.getInstance()
            cal.time = d

            return ReportData(idNr)
        }
    }
}