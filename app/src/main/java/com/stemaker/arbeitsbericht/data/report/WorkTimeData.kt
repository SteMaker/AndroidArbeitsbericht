package com.stemaker.arbeitsbericht.data.report

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.stemaker.arbeitsbericht.data.base.*
import com.stemaker.arbeitsbericht.data.dateStringToCalendar
import org.joda.time.*
import org.joda.time.format.PeriodFormatterBuilder
import java.util.*
import kotlin.Comparator

const val WORK_TIME_CONTAINER_VISIBILITY = "wtcVis"
const val WORK_TIME_CONTAINER = "wtc"
class WorkTimeContainerData(
    private val defaultEmployeeName: LiveData<String>
)
    : DataContainer<WorkTimeData>(WORK_TIME_CONTAINER)
{
    val visibility = DataElement<Boolean>(WORK_TIME_CONTAINER_VISIBILITY) { false }

    /*
    fun copyFromSerialized(w: WorkTimeContainerDataSerialized) {
        visibility.value = w.visibility
        clear()
        for(i in 0 until w.items.size) {
            val item = WorkTimeData()
            item.copyFromSerialized(w.items[i])
            add(item)
        }
    }
    */

    fun copyFromDb(w: WorkTimeContainerDb) {
        visibility.value = w.wtVisibility
        clear()
        for(element in w.wtItems) {
            val item = WorkTimeData(defaultEmployeeName)
            item.copyFromDb(element)
            add(item)
        }
    }

    fun copy(w: WorkTimeContainerData) {
        visibility.value = w.visibility.value
        clear()
        for(element in w.items) {
            val item = WorkTimeData(defaultEmployeeName)
            item.copy(element)
            add(item)
        }
    }

    fun addWorkTime(ref: WorkTimeData? = null): WorkTimeData {
        val wt = WorkTimeData(defaultEmployeeName)
        if(ref != null) {
            wt.clone(ref)
        }
        add(wt) // triggers LiveData update
        return wt
    }

    fun addWorkTime(def: DefaultValues): WorkTimeData {
        val wt = WorkTimeData(defaultEmployeeName)
        if(def.useDefaultDistance) wt.distance.value = def.defaultDistance
        if(def.useDefaultDriveTime) wt.driveTime.value = def.defaultDriveTime
        add(wt) // triggers LiveData update
        return wt
    }

    fun removeWorkTime(wt: WorkTimeData) {
        remove(wt) // triggers LiveData update
    }

    fun sortByDate() {
        val comparator = Comparator<WorkTimeData> { a: WorkTimeData, b: WorkTimeData ->
            a.date.value?.let { ita ->
                b.date.value?.let { itb ->
                    ita.time.compareTo(itb.time)
                }
            } ?: 0
        }
        sortWith(comparator)
    }
    // Would be nice to have this in the base class and only the Comparator in the specific class, but I didn't succeed to do so
    private fun sortWith(comparator: Comparator<WorkTimeData>): Unit {
        items.sortWith(comparator)
        containerModificationEvent.value = DataModificationEvent(DataModificationEvent.Type.CONTAINER_REORDER, this)
    }
}

const val WORK_TIME_DATE = "wtDate"
const val WORK_TIME_EMPLOYEES = "wtEmployees"
const val WORK_TIME_EMPLOYEE = "wtEmployee"
const val WORK_TIME_START_TIME = "wtStartTime"
const val WORK_TIME_END_TIME = "wtEndTime"
const val WORK_TIME_PAUSE_DURATION = "wtPauseDuration"
const val WORK_TIME_DRIVE_TIME = "wtDriveTime"
const val WORK_TIME_DISTANCE = "wtDistance"
const val WORK_TIME_DATA = "workTimeData"
class WorkTimeData(
    private val defaultEmployeeName: LiveData<String>)
    : DataObject(WORK_TIME_DATA)
{
    val date = DataElement<Calendar>(WORK_TIME_DATE) { Calendar.getInstance() }
    val employees = DataContainer<DataElement<String>>(WORK_TIME_EMPLOYEES).apply {
        add(DataElement<String>(WORK_TIME_EMPLOYEE) { defaultEmployeeName.value?:"" })
    }
    // Empty string as startTime will lead to pre-setting current time when clicking the edit button
    val startTime = DataElement<String>(WORK_TIME_START_TIME) { "" }
    // Empty string in endTime will lead to pre-setting current time when clicking the edit button
    val endTime = DataElement<String>(WORK_TIME_END_TIME) { "" }
    val pauseDuration = DataElement<String>( WORK_TIME_PAUSE_DURATION) { "00:00" }
    val driveTime = DataElement<String>( WORK_TIME_DRIVE_TIME) { "00:00" }
    var distance = DataElement<Int>( WORK_TIME_DISTANCE) { 0 }

    override val elements = listOf<DataBasicIf>(
        date, employees, startTime, endTime, pauseDuration, driveTime, distance
    )

    // Will be reworked as part of the "Stundenbericht" to use jodatime for all time objects
    // As a quick fix we'll do this ugly stuff
    val workDuration = MediatorLiveData<String>()

    private fun calcWorkDuration(): String {
        var duration: Duration
        try {
            val startHour = startTime.value!!.substring(0, 2).toInt()
            val startMinute = startTime.value!!.substring(3, 5).toInt()
            val endHour = endTime.value!!.substring(0, 2).toInt()
            val endMinute = endTime.value!!.substring(3, 5).toInt()
            val endOnNextDay = (endHour < startHour) || ((endHour == startHour) && (endMinute < startMinute))
            val startDate = DateTime(2000, 1, 1, startHour, startMinute)
            val endDate = DateTime(2000, 1, if (endOnNextDay) 2 else 1, endHour, endMinute)
            val pauseMinutes = pauseDuration.value!!.substring(0, 2).toInt() * 60 + pauseDuration.value!!.substring(3, 5).toInt()
            val pauseDuration = Duration((pauseMinutes * 60 * 1000).toLong())
            duration = Duration(startDate, endDate).minus(pauseDuration)
            // multiply by amount of employees
            if (employees.getSize() > 1) {
                duration = duration.multipliedBy(employees.getSize().toLong())
            }
        } catch (e: Exception) {
            return "00:00"
        }
        val formatter = PeriodFormatterBuilder()
            .printZeroAlways()
            .minimumPrintedDigits(2)
            .appendHours()
            .appendSuffix(":")
            .appendMinutes()
            .toFormatter();

        return formatter.print(duration.toPeriod().normalizedStandard(PeriodType.dayTime()))
    }

    init {
        workDuration.addSource(startTime) { workDuration.value = calcWorkDuration() }
        workDuration.addSource(endTime) { workDuration.value = calcWorkDuration() }
        workDuration.addSource(pauseDuration) { workDuration.value = calcWorkDuration() }
        workDuration.addSource(employees.containerModificationEvent) { workDuration.value = calcWorkDuration() }
    }


    fun addEmployee(): DataElement<String> {
        val emp = DataElement<String>(WORK_TIME_EMPLOYEE) { defaultEmployeeName.value?:"" }
        employees.add(emp)
        return emp
    }

    fun removeEmployee(emp: DataElement<String>) {
        employees.remove(emp)
    }

    /*
    fun copyFromSerialized(w: WorkTimeDataSerialized) {
        date.value = dateStringToCalendar(w.date)
        employees.clear()
        for(empSer in w.employees) {
            val emp = DataElement<String>(WORK_TIME_EMPLOYEE) { empSer }
            employees.add(emp)
        }
        startTime.value = w.startTime
        endTime.value = w.endTime
        pauseDuration.value = w.pauseDuration
        driveTime.value = w.driveTime
        distance.value = w.distance
    }
     */

    fun copyFromDb(w: WorkTimeContainerDb.WorkTimeDb) {
        date.value = dateStringToCalendar(w.wtDate)
        employees.clear() // because we always add one during construction
        for(empSer in w.wtEmployees) {
            val emp = DataElement<String>(WORK_TIME_EMPLOYEE) { empSer }
            employees.add(emp)
        }
        startTime.value = w.wtStartTime
        endTime.value = w.wtEndTime
        pauseDuration.value = w.wtPauseDuration
        driveTime.value = w.wtDriveTime
        distance.value = w.wtDistance
    }

    fun copy(w: WorkTimeData) {
        date.copy(w.date)
        employees.clear()
        for(empSer in w.employees) {
            val emp = DataElement<String>(WORK_TIME_EMPLOYEE) { empSer.value?:"" }
            employees.add(emp)
        }
        startTime.copy(w.startTime)
        endTime.copy(w.endTime)
        pauseDuration.copy(w.pauseDuration)
        driveTime.copy(w.driveTime)
        distance.copy(w.distance)
    }

    private fun incDateByOneWeekday(dateIn: Calendar): Calendar {
        var isWeekDay = true
        val dateOut = dateIn.clone() as Calendar
        while(isWeekDay) {
            dateOut.add(Calendar.DATE, 1)
            val dow = dateOut.get(Calendar.DAY_OF_WEEK)
            if(dow == Calendar.SATURDAY || dow == Calendar.SUNDAY)
                dateOut.add(Calendar.DATE, 1)
            else
                isWeekDay = false
        }
        return dateOut
    }

    fun clone(w: WorkTimeData) {
        date.value  = incDateByOneWeekday(w.date.value!!)
        employees.clear()
        for(empRef in w.employees.items) {
            val emp = DataElement<String>(WORK_TIME_EMPLOYEE) { empRef.value!! }
            employees.add(emp)
        }
        startTime.value = w.startTime.value!!
        endTime.value = w.endTime.value!!
        pauseDuration.value = w.pauseDuration.value!!
        driveTime.value = w.driveTime.value!!
        distance.value = w.distance.value!!
    }
}