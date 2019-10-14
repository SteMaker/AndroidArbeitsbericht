package com.stemaker.arbeitsbericht.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.serialization.json.Json
import java.util.*

class ReportData private constructor(val __id: Int = 0): ViewModel() {
    private val _id = MutableLiveData<Int>()
    val id: LiveData<Int>
        get() = _id

    private val _create_date = MutableLiveData<String>()
    val create_date: LiveData<String>
        get() = _create_date

    private val _change_date = MutableLiveData<String>()
    val change_date: LiveData<String>
        get() = _change_date

    var project = ProjectData()
    var bill = BillData()
    val workTimeContainer = WorkTimeContainerData()
    val workItemContainer = WorkItemContainerData()

    init {
        _id.value = __id
        _create_date.value = getCurrentDate()
        _change_date.value = _create_date.value
    }

    private fun copyFromSerialized(r: ReportDataSerialized) {
        _id.value = r.id
        _create_date.value = r.create_date
        _change_date.value = r.change_date
        project.copyFromSerialized(r.project)
        bill.copyFromSerialized(r.bill)
        workTimeContainer.copyFromSerialized(r.workTimeContainer)
        workItemContainer.copyFromSerialized(r.workItemContainer)
    }

    private fun getCurrentDate(): String {
        val d = Date()
        val cal = Calendar.getInstance()
        cal.time = d
        return cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2,'0') + "." +
                (cal.get(Calendar.MONTH)+1).toString().padStart(2,'0') + "." +
                cal.get(Calendar.YEAR).toString().padStart(4,'0')
    }

    fun updateLastChangeDate() {
        val d = Date()
        val cal = Calendar.getInstance()
        cal.time = d
        _change_date.value = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2,'0') + "." +
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

        fun createReport(id: Int): ReportData {
            return ReportData(id)
        }
    }
}