package com.stemaker.arbeitsbericht.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stemaker.arbeitsbericht.storageHandler
import java.util.*

class WorkTimeContainerData(): ViewModel() {
    val items = mutableListOf<WorkTimeData>()

    fun copyFromSerialized(w: WorkTimeContainerDataSerialized) {
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

    val employees = mutableListOf<MutableLiveData<String>>().apply { add(MutableLiveData<String>().apply { value = storageHandler().configuration.employeeName } ) }

    val startTime = MutableLiveData<String>().apply { value =  "00:00"}

    val endTime = MutableLiveData<String>().apply { value =  "00:00"}

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
        val emp = MutableLiveData<String>().apply { value = storageHandler().configuration.employeeName }
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

    fun clone(w: WorkTimeData) {
        date.value = w.date.value!!
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