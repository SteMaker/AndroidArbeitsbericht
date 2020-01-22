package com.stemaker.arbeitsbericht.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stemaker.arbeitsbericht.Configuration
import com.stemaker.arbeitsbericht.configuration
import com.stemaker.arbeitsbericht.storageHandler
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

    fun addWorkTime(ref: WorkTimeData? = null): WorkTimeData {
        val wt = WorkTimeData()
        if(ref != null) {
            wt.clone(ref)
        }
        items.add(wt)
        return wt
    }

    fun removeWorkTime(wt: WorkTimeData) {
        items.remove(wt)
    }
}

class WorkTimeData: ViewModel() {
    val date = MutableLiveData<String>().apply { value =  getCurrentDate()}

    val employees = mutableListOf<MutableLiveData<String>>().apply { add(MutableLiveData<String>().apply { value = configuration().employeeName } ) }

    val startTime = MutableLiveData<String>().apply { value =  ""} // Empty string will lead to pre-setting current time when clicking the edit button

    val endTime = MutableLiveData<String>().apply { value =  ""} // Empty string will lead to pre-setting current time when clicking the edit button

    val driveTime = MutableLiveData<String>().apply { value =  "00:00"}

    var distance = MutableLiveData<Int>().apply { value = 0 }

    private fun getCurrentDate(): String {
        val d = Date()
        val cal = Calendar.getInstance()
        cal.time = d
        return cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2,'0') + "." +
                (cal.get(Calendar.MONTH)+1).toString().padStart(2,'0') + "." +
                cal.get(Calendar.YEAR).toString().padStart(4,'0')
    }

    fun addEmployee(): MutableLiveData<String> {
        val emp = MutableLiveData<String>().apply { value = configuration().employeeName }
        employees.add(emp)
        return emp
    }

    fun removeEmployee(emp: MutableLiveData<String>) {
        employees.remove(emp)
    }

    fun copyFromSerialized(w: WorkTimeDataSerialized) {
        date.value = w.date
        employees.clear()
        for(empSer in w.employees) {
            val emp = MutableLiveData<String>().apply { value = empSer }
            employees.add(emp)
        }
        startTime.value = w.startTime
        endTime.value = w.endTime
        driveTime.value = w.driveTime
        distance.value = w.distance
    }

    fun incDateByOneWeekday(dateIn: String): String {
        var isWeekDay = true
        val cal = Calendar.getInstance()
        cal.set(dateIn.substring(6,10).toInt(), dateIn.substring(3,5).toInt()-1, dateIn.substring(0,2).toInt())
        while(isWeekDay) {
            cal.add(Calendar.DATE, 1)
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            if(dow == Calendar.SATURDAY || dow == Calendar.SUNDAY)
                cal.add(Calendar.DATE, 1)
            else
                isWeekDay = false
        }
        return cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2,'0') + "." +
                (cal.get(Calendar.MONTH)+1).toString().padStart(2,'0') + "." +
                cal.get(Calendar.YEAR).toString().padStart(4,'0')
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
        driveTime.value = w.driveTime.value!!
        distance.value = w.distance.value!!
    }
}