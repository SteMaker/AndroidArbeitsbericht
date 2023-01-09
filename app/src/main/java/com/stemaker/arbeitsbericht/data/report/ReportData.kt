package com.stemaker.arbeitsbericht.data.report

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.base.DataBasicIf
import com.stemaker.arbeitsbericht.data.base.DataElement
import com.stemaker.arbeitsbericht.data.base.DataObject
import com.stemaker.arbeitsbericht.data.configuration.configuration
import kotlinx.serialization.json.Json
import java.util.*

//Liste mit Änderungenm, die noch mindestens nötig sind. Explizit nicht als Kommentar, um
//darauf aufmerksam zu machen
//- Die Hochfahrphase durchgehen -> geht das theoretisch?
//- Überprüfen, wie die dictionaries funktionieren
//- Teste ob beim beenden der app auf eine noch laufende coroutine gewartet wird
//- Weg von GlobalScope
//- Timer für jeden report verwenden. Bei modify -> zurücksetzen, bei timeout -> save
//- App für clients und configuration ähnlich erweitern, wie für reports, damit die gespeichert werden beim schließen

private const val TAG="ReportData"
private const val REPORT = "report"

class ReportData private constructor(var cnt: Int = 0): DataObject(REPORT)
{
    // Purpose of this var is to remember if the report needs to be stored
    var modified: Boolean = false

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
    val state = DataElement<ReportState>("state") { ReportState.IN_WORK }
    var project = ProjectData()
    var bill = BillData()
    val workTimeContainer = WorkTimeContainerData()
    val workItemContainer = WorkItemContainerData()
    val materialContainer = MaterialContainerData()
    val lumpSumContainer = LumpSumContainerData()
    val photoContainer = PhotoContainerData()
    val signatureData = SignatureData()
    val defaultValues = DefaultValues()
    val id = MediatorLiveData<String> ()

    override val elements = listOf<DataBasicIf>(
        state, project, bill, workTimeContainer, workItemContainer, materialContainer, lumpSumContainer,
        photoContainer, signatureData
    )

    init {
        /* Build the observers that keep the ID up-to-date */
        _create_date.value = getCurrentDate()
        id.addSource(create_date) {
            id.value = buildId()
        }
        id.addSource(project.name) {
            id.value = buildId()
        }
        id.addSource(project.extra1) {
            id.value = buildId()
        }
        id.addSource(configuration().reportIdPattern) {
            id.value = buildId()
        }

        // Any change within the report will be propagated here
        dataModificationEvent.observeForever { value ->
            Log.d(TAG, "${value.elem.elementName} has been modified")
            modified = true
        }
    }

    private fun buildId(): String {
        var string = configuration().reportIdPattern.value?:""
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
        project.name.value?.let { string = string.replace("%k", it) }
        project.extra1.value?.let { string = string.replace("%z", it) }

        // Remove spaces and other problematic chars
        val regex2 = "[^a-zA-Z0-9öÖäÄüÜ\\-_]".toRegex()
        string = regex2.replace(string) { m ->
            "_"
        }

        return string
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
        defaultValues.copyFromDb(r.defaultValues)
        modified = false
        Log.d(TAG, "copyFromDb done for report ${cnt}")
    }


    fun copy(origin: ReportData, copyDate: Boolean = false) {
        if(copyDate)
            _create_date.value = origin.create_date.value
        state.copy(origin.state)
        project.copy(origin.project)
        bill.copy(origin.bill)
        workTimeContainer.copy(origin.workTimeContainer)
        workItemContainer.copy(origin.workItemContainer)
        materialContainer.copy(origin.materialContainer)
        lumpSumContainer.copy(origin.lumpSumContainer)
        photoContainer.copy(origin.photoContainer) // This is actually empty / not doing anything!!
        signatureData.copy(origin.signatureData) // This is actually empty / not doing anything!!
        defaultValues.copy(origin.defaultValues)
        modified = true
        Log.d(TAG, "copyFromReport done for report ${cnt}")
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

        fun getReportFromReport(origin: ReportData) {

        }

        fun getJsonFromReport(r: ReportData): String {
            val serialized = ReportDataSerialized()
            serialized.copyFromData(r) // Copy from data object into serialized object
            return Json.encodeToString(ReportDataSerialized.serializer(), serialized)
        }

        fun createReport(cnt: Int): ReportData {
            val r = ReportData(cnt)
            r.modified = true
            return r
        }
    }
}