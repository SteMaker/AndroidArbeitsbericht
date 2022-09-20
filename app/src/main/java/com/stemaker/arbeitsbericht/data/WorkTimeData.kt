package com.stemaker.arbeitsbericht.data

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*

class WorkTimeContainerData(): ViewModel() {
    val items = mutableListOf<WorkTimeData>()
    val visibility = MutableLiveData<Boolean>().apply { value = false }

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
        items.add(wt)
        return wt
    }

    fun addWorkTime(def: DefaultValues): WorkTimeData {
        val wt = WorkTimeData()
        if(def.useDefaultDistance) wt.distance.value = def.defaultDistance
        if(def.useDefaultDriveTime) wt.driveTime.value = def.defaultDriveTime
        items.add(wt)
        return wt
    }

    fun removeWorkTime(wt: WorkTimeData) {
        items.remove(wt)
    }
}

class WorkTimeData: ViewModel() {
    val date = MutableLiveData<Calendar>().apply { value =  Calendar.getInstance() }

    val employees = mutableListOf<MutableLiveData<String>>().apply { add(MutableLiveData<String>().apply { value = configuration().employeeName } ) }

    val startTime = MutableLiveData<String>().apply { value =  ""} // Empty string will lead to pre-setting current time when clicking the edit button

    val endTime = MutableLiveData<String>().apply { value =  ""} // Empty string will lead to pre-setting current time when clicking the edit button

    val pauseDuration = MutableLiveData<String>().apply { value =  "00:00"}

    // Will be reworked as part of the "Stundenbericht" to use jodatime for all time objects
    // As a quick fix we'll do this ugly stuff
    val workDuration = MediatorLiveData<String>()

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

    val driveTime = MutableLiveData<String>().apply { value =  "00:00"}

    var distance = MutableLiveData<Int>().apply { value = 0 }

    fun addEmployee(): MutableLiveData<String> {
        val emp = MutableLiveData<String>().apply { value = configuration().employeeName }
        employees.add(emp)
        workDuration.value = calcWorkDuration()
        return emp
    }

    fun removeEmployee(emp: MutableLiveData<String>) {
        employees.remove(emp)
        workDuration.value = calcWorkDuration()
    }

    fun copyFromSerialized(w: WorkTimeDataSerialized) {
        date.value = dateStringToCalendar(w.date)
        employees.clear()
        for(empSer in w.employees) {
            val emp = MutableLiveData<String>().apply { value = empSer }
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
            val emp = MutableLiveData<String>().apply { value = empSer }
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
            val emp = MutableLiveData<String>().apply { value = empRef.value!! }
            employees.add(emp)
        }
        startTime.value = w.startTime.value!!
        endTime.value = w.endTime.value!!
        pauseDuration.value = w.pauseDuration.value!!
        driveTime.value = w.driveTime.value!!
        distance.value = w.distance.value!!
    }
}