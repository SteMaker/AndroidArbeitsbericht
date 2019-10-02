package com.stemaker.arbeitsbericht

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*
import kotlinx.serialization.*

@Serializable
class WorkTime(var date: String = "", var employee: String = storageHandler().configuration.employeeName,
    var duration: String = "00:00", var driveTime: String = "00:00", var distance: Int = 0) {
    init {
        if(date == "") {
            val d = Date()
            val cal = Calendar.getInstance()
            cal.time = d
            date = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0') + "." +
                    (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0') + "." +
                    cal.get(Calendar.YEAR).toString().padStart(4, '0')
        }
    }
}

@Serializable
class WorkItem(var item: String = "") {
}

@Serializable
class LumpSum(var item: String = "") {
}

@Serializable
class Material(var item: String = "", var amount: Int = 0) {
}

@Serializable
class Photo(var file: String = "", var description: String = "") {
}

@Serializable
class BillDataSerialized {
    var name: String = ""
    var street: String = ""
    var zip: String = ""
    var city: String = ""

    fun copyFromBill(b: BillData) {
        name = b.name.value!!
        street = b.street.value!!
        zip = b.zip.value!!
        city = b.city.value!!
    }
}

class BillData(b: BillDataSerialized) : ViewModel() {
    val name = MutableLiveData<String>().apply { value =  b.name}
    val street = MutableLiveData<String>().apply { value =  b.street}
    val zip = MutableLiveData<String>().apply { value =  b.zip}
    val city = MutableLiveData<String>().apply { value =  b.city}
}

@Serializable
class ProjectDataSerialized {
    var name: String = ""
    var extra1: String = ""

    fun copyFromProject(p: ProjectData) {
        name = p.name.value!!
        extra1 = p.extra1.value!!
    }
}

class ProjectData(p: ProjectDataSerialized) : ViewModel() {
    val name = MutableLiveData<String>().apply { value = p.name }
    val extra1 = MutableLiveData<String>().apply { value = p.extra1 }
}

@Serializable
class ReportDataSerialized(var id: Int=0) {
    var create_date: String
    var change_date: String
    var project = ProjectDataSerialized()
    var bill = BillDataSerialized()

    init {
        val d = Date()
        val cal = Calendar.getInstance()
        cal.time = d
        create_date = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2,'0') + "." +
                (cal.get(Calendar.MONTH)+1).toString().padStart(2,'0') + "." +
                cal.get(Calendar.YEAR).toString().padStart(4,'0')
        change_date = create_date
    }

    fun copyFromReport(r: ReportData) {
        id = r.id.value!!
        create_date = r.create_date.value!!
        change_date = r.change_date.value!!
        project.copyFromProject(r.project)
        bill.copyFromBill(r.bill)
    }
}
class ReportData(r: ReportDataSerialized) {
    val id = MutableLiveData<Int>().apply { value = r.id }
    var create_date = MutableLiveData<String>().apply { value = r.create_date }
    var change_date = MutableLiveData<String>().apply { value = r.change_date }

    fun updateLastChangeDate() {
        val d = Date()
        val cal = Calendar.getInstance()
        cal.time = d
        change_date.value = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2,'0') + "." +
                (cal.get(Calendar.MONTH)+1).toString().padStart(2,'0') + "." +
                cal.get(Calendar.YEAR).toString().padStart(4,'0')
    }

    var project = ProjectData(r.project)
    var bill = BillData(r.bill)
}
