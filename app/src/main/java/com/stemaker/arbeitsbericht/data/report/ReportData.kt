package com.stemaker.arbeitsbericht.data.report

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.configuration.configuration
import kotlinx.serialization.json.Json
import com.stemaker.arbeitsbericht.data.base.DataSimple
import com.stemaker.arbeitsbericht.data.base.ElementChangeEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

private const val TAG="ReportData"
const val FILTER_IMPACTING_PROPERTY = 1

class ReportData private constructor(var cnt: Int = 0):
    PropertyChangeListener
{
    // Purpose of this var is to remember if the report needs to be stored
    private var modified: Boolean = false

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
    val state = DataSimple<ReportState>(ReportState.IN_WORK, "state")
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
    val filterMediator = MediatorLiveData<Boolean>()

    override fun propertyChange(ev: PropertyChangeEvent) {
        modified = true
    }

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

        // Build the observers that impact the filter
        filterMediator.addSource(project.name) {  update(FILTER_IMPACTING_PROPERTY) }
        filterMediator.addSource(project.extra1) {  update(FILTER_IMPACTING_PROPERTY) }
        filterMediator.addSource(state) {  update(FILTER_IMPACTING_PROPERTY) }

        // Build the observers that inform about changes
        project.addObserver(this)
        bill.addObserver(this)
        workTimeContainer.addObserver(this)
    }

    private val observers = mutableListOf<androidx.databinding.Observable.OnPropertyChangedCallback>()
    override fun addOnPropertyChangedCallback(observer: androidx.databinding.Observable.OnPropertyChangedCallback?) {
        observer?.let { observers.add(it) }
    }

    override fun removeOnPropertyChangedCallback(observer: androidx.databinding.Observable.OnPropertyChangedCallback?) {
        observer?.let { observers.remove(it) }
    }

    fun update(propertyId: Int) {
        observers.forEach {
            it.onPropertyChanged(this, propertyId)
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
            val r = ReportData(cnt)
            r.modified = true
            return r
        }
    }
}