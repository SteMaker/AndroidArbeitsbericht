package com.stemaker.arbeitsbericht.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stemaker.arbeitsbericht.R
import kotlinx.serialization.json.Json
import java.util.*

private const val TAG="ReportData"

class ReportData private constructor(var cnt: Int = 0): ViewModel() {
    private var _id = MutableLiveData<String>()
    val idLive: LiveData<String>
        get() { return MutableLiveData<String>().apply { value = id } }

    val id: String
        get() {
            // TODO: Check how often this is executed, performance impact?
            var string = configuration().reportIdPattern
            // Replace %<n>c -> running counter
            val regex = """(%c|%[0-9]c)""".toRegex()
            string = regex.replace(string) { m ->
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

    enum class ReportState(v: Int) {
        IN_WORK(0), ON_HOLD(1), DONE(2), ARCHIVED(3);

        companion object {
            fun fromInt(value: Int) = values().first() { it.ordinal == value }
            fun toInt(s: ReportState) = s.ordinal
            fun toStringId(s: ReportState): Int {
                return when(s) {
                    IN_WORK -> R.string.in_work
                    ON_HOLD -> R.string.on_hold
                    DONE -> R.string.done
                    ARCHIVED -> R.string.archived
                }
            }
        }
    }
    val state = MutableLiveData<ReportState>().apply { value = ReportState.IN_WORK }

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
        state.value = ReportState.fromInt(r.state)
        project.copyFromSerialized(r.project)
        bill.copyFromSerialized(r.bill)
        workTimeContainer.copyFromSerialized(r.workTimeContainer)
        workItemContainer.copyFromSerialized(r.workItemContainer)
        materialContainer.copyFromSerialized(r.materialContainer)
        lumpSumContainer.copyFromSerialized(r.lumpSumContainer)
        photoContainer.copyFromSerialized(r.photoContainer)
        signatureData.copyFromSerialized(r.signatureData)
    }

    private fun copyFromDb(r: ReportDb) {
        cnt = r.cnt
        _create_date.value = r.create_date
        state.value = ReportState.fromInt(r.state)
        project.copyFromDb(r.project)
        bill.copyFromDb(r.bill)
        workTimeContainer.copyFromDb(r.workTimeContainer)
        workItemContainer.copyFromDb(r.workItemContainer)
        materialContainer.copyFromDb(r.materialContainer)
        lumpSumContainer.copyFromDb(r.lumpSumContainer)
        photoContainer.copyFromDb(r.photoContainer)
        signatureData.copyFromDb(r.signatures)

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
            val serialized = Json.decodeFromString(ReportDataSerialized.serializer(), jsonData)
            val report = ReportData()
            report.copyFromSerialized(serialized)
            return report
        }

        fun getReportFromDb(rDb: ReportDb, r: ReportData) {
            r.copyFromDb(rDb)
        }

        fun getJsonFromReport(r: ReportData): String {
            val serialized = ReportDataSerialized()
            serialized.copyFromData(r) // Copy from data object into serialized object
            return Json.encodeToString(ReportDataSerialized.serializer(), serialized)
        }

        fun createReport(cnt: Int): ReportData {
            return ReportData(cnt)
        }
    }
}