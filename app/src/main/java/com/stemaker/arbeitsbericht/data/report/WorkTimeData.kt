package com.stemaker.arbeitsbericht.data.report

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.stemaker.arbeitsbericht.data.base.DataContainer
import com.stemaker.arbeitsbericht.data.base.DataObject
import com.stemaker.arbeitsbericht.data.base.DataSimple
import com.stemaker.arbeitsbericht.data.base.DataSimpleList
import com.stemaker.arbeitsbericht.data.configuration.configuration
import com.stemaker.arbeitsbericht.data.dateStringToCalendar
import java.util.*

const val WORK_TIME_CONTAINER_VISIBILITY = "wtcVis"
const val WORK_TIME_CONTAINER = "wtc"
class WorkTimeContainerData():
    DataContainer<WorkTimeData>(WORK_TIME_CONTAINER) {
    /* @TODO this is not available as event right now, except when directly registering on the member */
    val visibility = DataSimple<Boolean>(false, WORK_TIME_CONTAINER_VISIBILITY)

    fun copyFromSerialized(w: WorkTimeContainerDataSerialized) {
        visibility.value = w.visibility
        items.clear()
        for(i in 0 until w.items.size) {
            val item = WorkTimeData()
            item.copyFromSerialized(w.items[i])
            items.add(item)
        }
    }

    fun copyFromDb(w: WorkTimeContainerDb) {
        visibility.value = w.wtVisibility
        items.clear()
        for(element in w.wtItems) {
            val item = WorkTimeData()
            item.copyFromDb(element)
            items.add(item)
        }
    }

    fun addWorkTime(ref: WorkTimeData? = null): WorkTimeData {
        val wt = WorkTimeData()
        if(ref != null) {
            wt.clone(ref)
        }
        add(wt)
        return wt
    }

    fun addWorkTime(def: DefaultValues): WorkTimeData {
        val wt = WorkTimeData()
        if(def.useDefaultDistance) wt.distance.value = def.defaultDistance
        if(def.useDefaultDriveTime) wt.driveTime.value = def.defaultDriveTime
        add(wt)
        return wt
    }

    fun removeWorkTime(wt: WorkTimeData) {
        remove(wt)
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
const val WORK_TIME = "workTime"
class WorkTimeData(): DataObject(WORK_TIME)  {

    val date = DataSimple<Calendar>(Calendar.getInstance(), WORK_TIME_DATE)
    val employees = DataSimpleList<DataSimple<String>>(WORK_TIME_EMPLOYEES).apply { add(DataSimple<String>(configuration().employeeName, WORK_TIME_EMPLOYEE)) }
    // Empty string as startTime will lead to pre-setting current time when clicking the edit button
    val startTime = DataSimple<String>("", WORK_TIME_START_TIME)
    // Empty string in endTime will lead to pre-setting current time when clicking the edit button
    val endTime = DataSimple<String>("", WORK_TIME_END_TIME)
    val pauseDuration = DataSimple<String>("00:00", WORK_TIME_PAUSE_DURATION)
    val driveTime = DataSimple<String>("00:00", WORK_TIME_DRIVE_TIME)
    var distance = DataSimple<Int>(0, WORK_TIME_DISTANCE)

    init {
        registerElement(date)
        registerElement(employees)
        registerElement(startTime)
        registerElement(endTime)
        registerElement(pauseDuration)
        registerElement(driveTime)
        registerElement(distance)
    }
    // Will be reworked as part of the "Stundenbericht" to use jodatime for all time objects
    // As a quick fix we'll do this ugly stuff
    private val workDuration = MediatorLiveData<String>()

    fun calcWorkDuration(): String {
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
    }


    fun addEmployee(): DataSimple<String> {
        val emp = DataSimple<String>(configuration().employeeName, WORK_TIME_EMPLOYEE)
        employees.add(emp)
        workDuration.value = calcWorkDuration()
        return emp
    }

    fun removeEmployee(emp: DataSimple<String>) {
        employees.remove(emp)
        workDuration.value = calcWorkDuration()
    }

    fun copyFromSerialized(w: WorkTimeDataSerialized) {
        date.value = dateStringToCalendar(w.date)
        employees.clear()
        for(empSer in w.employees) {
            val emp = DataSimple<String>(empSer, WORK_TIME_EMPLOYEE)
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
            val emp = DataSimple<String>(empSer, WORK_TIME_EMPLOYEE)
            employees.add(emp)
        }
        startTime.value = w.wtStartTime
        endTime.value = w.wtEndTime
        pauseDuration.value = w.wtPauseDuration
        driveTime.value = w.wtDriveTime
        distance.value = w.wtDistance
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
        for(empRef in w.employees) {
            val emp = DataSimple<String>(empRef.value!!, WORK_TIME_EMPLOYEE)
            employees.add(emp)
        }
        startTime.value = w.startTime.value!!
        endTime.value = w.endTime.value!!
        pauseDuration.value = w.pauseDuration.value!!
        driveTime.value = w.driveTime.value!!
        distance.value = w.distance.value!!
    }
}