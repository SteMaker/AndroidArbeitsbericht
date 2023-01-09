package com.stemaker.arbeitsbericht.data.report

import androidx.lifecycle.MediatorLiveData
import com.stemaker.arbeitsbericht.data.base.*
import com.stemaker.arbeitsbericht.data.configuration.configuration
import com.stemaker.arbeitsbericht.data.dateStringToCalendar
import java.util.*

const val WORK_TIME_CONTAINER_VISIBILITY = "wtcVis"
const val WORK_TIME_CONTAINER = "wtc"
class WorkTimeContainerData:
    DataContainer<WorkTimeData>(WORK_TIME_CONTAINER) {
    val visibility = DataElement<Boolean>(WORK_TIME_CONTAINER_VISIBILITY) { false }

    fun copyFromSerialized(w: WorkTimeContainerDataSerialized) {
        visibility.value = w.visibility
        clear()
        for(i in 0 until w.items.size) {
            val item = WorkTimeData()
            item.copyFromSerialized(w.items[i])
            add(item)
        }
    }

    fun copyFromDb(w: WorkTimeContainerDb) {
        visibility.value = w.wtVisibility
        clear()
        for(element in w.wtItems) {
            val item = WorkTimeData()
            item.copyFromDb(element)
            add(item)
        }
    }

    fun copy(w: WorkTimeContainerData) {
        visibility.value = w.visibility.value
        clear()
        for(element in w.items) {
            val item = WorkTimeData()
            item.copy(element)
            add(item)
        }
    }

    fun addWorkTime(ref: WorkTimeData? = null): WorkTimeData {
        val wt = WorkTimeData()
        if(ref != null) {
            wt.clone(ref)
        }
        add(wt) // triggers LiveData update
        return wt
    }

    fun addWorkTime(def: DefaultValues): WorkTimeData {
        val wt = WorkTimeData()
        if(def.useDefaultDistance) wt.distance.value = def.defaultDistance
        if(def.useDefaultDriveTime) wt.driveTime.value = def.defaultDriveTime
        add(wt) // triggers LiveData update
        return wt
    }

    fun removeWorkTime(wt: WorkTimeData) {
        remove(wt) // triggers LiveData update
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
class WorkTimeData: DataObject(WORK_TIME_DATA) {

    val date = DataElement<Calendar>(WORK_TIME_DATE) { Calendar.getInstance() }
    val employees = DataContainer<DataElement<String>>(WORK_TIME_EMPLOYEES).apply {
        add(DataElement<String>(WORK_TIME_EMPLOYEE) { configuration().employeeName })
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
        var h: Int
        var m: Int
        try {
            val etH = endTime.value!!.substring(0,2).toInt()
            val etM = endTime.value!!.substring(3,5).toInt()
            val stH = startTime.value!!.substring(0,2).toInt()
            val stM = startTime.value!!.substring(3,5).toInt()
            val pH = pauseDuration.value!!.substring(0,2).toInt()
            val pM = pauseDuration.value!!.substring(3,5).toInt()
            h = etH - stH
            if(h < 0)
                h += 24
            m = etM - stM
            if(m < 0) {
                m += 60
                h--
            }
            h -= pH
            m -= pM
            if(m < 0) {
                m += 60
                h--
            }
            // multiply by amount of employees
            if(employees.size > 1) {
                m *= employees.size
                h *= employees.size
                while(m >= 60) {
                    m -= 60
                    h++
                }
            }
        } catch(e: Exception) {
            return "00:00"
        }
        return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
    }

    init {
        workDuration.addSource(startTime) { workDuration.value = calcWorkDuration() }
        workDuration.addSource(endTime) { workDuration.value = calcWorkDuration() }
        workDuration.addSource(pauseDuration) { workDuration.value = calcWorkDuration() }
        workDuration.addSource(employees.containerModificationEvent) { workDuration.value = calcWorkDuration() }
    }


    fun addEmployee(): DataElement<String> {
        val emp = DataElement<String>(WORK_TIME_EMPLOYEE) { configuration().employeeName }
        employees.add(emp)
        return emp
    }

    fun removeEmployee(emp: DataElement<String>) {
        employees.remove(emp)
    }

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

    fun copyFromDb(w: WorkTimeContainerDb.WorkTimeDb) {
        date.value = dateStringToCalendar(w.wtDate)
        employees.clear()
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