package com.stemaker.arbeitsbericht.data.hoursreport

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.serialization.json.Json
import java.util.*

private const val TAG = "HoursReportData"

class HoursReportData private constructor(): ViewModel() {

    var id = System.currentTimeMillis()

    private val _date = MutableLiveData<String>()
    val date: LiveData<String>
        get() = _date

    val project = MutableLiveData<String>().apply { value = "" }

    val projectNr = MutableLiveData<String>().apply { value = "" }

    val workItems = mutableListOf<MutableLiveData<String>>().apply { add(MutableLiveData<String>().apply { value = "" } ) }

    val startTime = MutableLiveData<String>().apply { value =  ""} // Empty string will lead to pre-setting current time when clicking the edit button

    val endTime = MutableLiveData<String>().apply { value =  ""} // Empty string will lead to pre-setting current time when clicking the edit button

    var lastStoreHash: Int = 0

    init {
        _date.value = getCurrentDate()
    }

    fun addWorkItem(): MutableLiveData<String> {
        val wi = MutableLiveData<String>().apply { value = "" }
        workItems.add(wi)
        return wi
    }

    fun removeWorkItem(wi: MutableLiveData<String>) {
        workItems.remove(wi)
    }

    private fun copyFromSerialized(r: HoursReportDataSerialized) {
        id = r.id
        _date.value = r.date
        project.value = r.project
        projectNr.value = r.projectNr
        workItems.clear()
        for(wiSer in r.workItems) {
            val wi = MutableLiveData<String>().apply { value = wiSer }
            workItems.add(wi)
        }
        startTime.value = r.startTime
        endTime.value = r.endTime
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
        fun getReportFromJson(jsonData: String): HoursReportData {
            val serialized = Json.parse(HoursReportDataSerialized.serializer(), jsonData)
            val report = HoursReportData()
            report.copyFromSerialized(serialized)
            return report
        }

        fun getJsonFromReport(r: HoursReportData): String {
            val serialized = HoursReportDataSerialized()
            serialized.copyFromData(r) // Copy from data object into serialized object
            val json: String = Json.stringify(HoursReportDataSerialized.serializer(), serialized)
            return json
        }

        fun createReport(): HoursReportData {
            return HoursReportData()
        }
    }
}
