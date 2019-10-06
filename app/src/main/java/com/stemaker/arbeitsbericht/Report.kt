package com.stemaker.arbeitsbericht

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json

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
class WorkTimeDataSerialized() {
    var date: String = ""
    var employee: String = storageHandler().configuration.employeeName
    var duration: String = "00:00"
    var driveTime: String = "00:00"
    var distance: Int = 0


    fun copyFromWorkTime(w: WorkTimeData) {
        date = w.date.value!!
        employee = w.employee.value!!
        duration = w.duration.value!!
        driveTime = w.driveTime.value!!
        distance = w.distance.value!!
    }

    fun copyToWorkTime(w: WorkTimeData) {
        w.date.value = date
        w.employee.value = employee
        w.duration.value = duration
        w.driveTime.value = driveTime
        w.distance.value = distance
    }
}

class WorkTimeData() {
    val date = MutableLiveData<String>().apply { value =  getCurrentDate()}
    val employee = MutableLiveData<String>().apply { value =  storageHandler().configuration.employeeName}
    val duration = MutableLiveData<String>().apply { value =  "00:00"}
    val driveTime = MutableLiveData<String>().apply { value =  "00:00"}
    val distance = MutableLiveData<Int>().apply { value =  0}

    private fun getCurrentDate(): String {
        val d = Date()
        val cal = Calendar.getInstance()
        cal.time = d
        return cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2,'0') + "." +
                (cal.get(Calendar.MONTH)+1).toString().padStart(2,'0') + "." +
                cal.get(Calendar.YEAR).toString().padStart(4,'0')
    }
}

@Serializable
class WorkTimeContainerDataSerialized() {
    val items = mutableListOf<WorkTimeDataSerialized>()

    fun copyFromWorkTimeContainer(w: WorkTimeContainerData) {
        items.clear()
        for(i in 0 until w.items.size) {
            val item = WorkTimeDataSerialized()
            item.copyFromWorkTime(w.items[i])
            items.add(item)
        }
    }

    fun copyToWorkTimeContainer(w: WorkTimeContainerData) {
        w.items.clear()
        for(i in 0 until items.size) {
            val item = WorkTimeData()
            items[i].copyToWorkTime(item)
            w.items.add(item)
        }
    }
}

class WorkTimeContainerData() {
    val items = mutableListOf<WorkTimeData>()
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

    fun copyToBill(b: BillData) {
        b.name.value = name
        b.street.value = street
        b.zip.value = zip
        b.city.value = city
    }
}

class BillData() : ViewModel() {
    val name = MutableLiveData<String>().apply { value = ""}
    val street = MutableLiveData<String>().apply { value = ""}
    val zip = MutableLiveData<String>().apply { value = ""}
    val city = MutableLiveData<String>().apply { value = ""}
}

@Serializable
class ProjectDataSerialized {
    var name: String = ""
    var extra1: String = ""

    fun copyFromProject(p: ProjectData) {
        name = p.name.value!!
        extra1 = p.extra1.value!!
    }

    fun copyToProject(p: ProjectData) {
        p.name.value = name
        p.extra1.value = extra1
    }
}

class ProjectData() : ViewModel() {
    val name = MutableLiveData<String>().apply { value = "" }
    val extra1 = MutableLiveData<String>().apply { value = "" }
}

@Serializable
private class ReportDataSerialized() {
    var id: Int = 0
    var create_date: String = "00:00"
    var change_date: String = "00:00"
    var project = ProjectDataSerialized()
    var bill = BillDataSerialized()
    val workTimeContainer = WorkTimeContainerDataSerialized()

    fun copyFromReport(r: ReportData) {
        id = r.id.value!!
        create_date = r.create_date.value!!
        change_date = r.change_date.value!!
        project.copyFromProject(r.project)
        bill.copyFromBill(r.bill)
        workTimeContainer.copyFromWorkTimeContainer(r.workTimeContainer)
    }

    fun copyToReport(r: ReportData) {
        r.id.value = id
        r.create_date.value = create_date
        r.change_date.value = change_date
        project.copyToProject(r.project)
        bill.copyToBill(r.bill)
        workTimeContainer.copyToWorkTimeContainer(r.workTimeContainer)
    }
}

class ReportData private constructor(val _id: Int = 0) {
    val id = MutableLiveData<Int>().apply { value = _id }
    var create_date = MutableLiveData<String>().apply { value = getCurrentDate() }
    var change_date = MutableLiveData<String>().apply { value = create_date.value }
    var project = ProjectData()
    var bill = BillData()
    val workTimeContainer = WorkTimeContainerData()

    private fun getCurrentDate(): String {
        val d = Date()
        val cal = Calendar.getInstance()
        cal.time = d
        return cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2,'0') + "." +
                (cal.get(Calendar.MONTH)+1).toString().padStart(2,'0') + "." +
                cal.get(Calendar.YEAR).toString().padStart(4,'0')
    }

    fun updateLastChangeDate() {
        val d = Date()
        val cal = Calendar.getInstance()
        cal.time = d
        change_date.value = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2,'0') + "." +
                (cal.get(Calendar.MONTH)+1).toString().padStart(2,'0') + "." +
                cal.get(Calendar.YEAR).toString().padStart(4,'0')
    }

    fun addWorkTime(): WorkTimeData {
        val wt = WorkTimeData()
        workTimeContainer.items.add(wt)
        return wt
    }

    companion object {
        fun getReportFromJson(jsonData: String): ReportData {
            val serialized = Json.parse(ReportDataSerialized.serializer(), jsonData)
            val report = ReportData()
            serialized.copyToReport(report)
            return report
        }

        fun getJsonFromReport(r: ReportData): String {
            val serialized = ReportDataSerialized()
            serialized.copyFromReport(r) // Copy from data object into serialized object
            val json: String = Json.stringify(ReportDataSerialized.serializer(), serialized)
            return json
        }

        fun createReport(id: Int): ReportData {
            return ReportData(id)
        }
    }
}
