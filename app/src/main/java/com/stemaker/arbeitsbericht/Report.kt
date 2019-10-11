package com.stemaker.arbeitsbericht

import android.util.Log
import android.widget.EditText
import androidx.databinding.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.google.android.material.textfield.TextInputEditText
import java.util.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.lang.NumberFormatException

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


    fun copyFromData(w: WorkTimeData) {
        Log.d("Arbeitsbericht.WorkTimeDataSerialized.copyFromData", "called")
        date = w.date.value!!
        employee = w.employee.value!!
        duration = w.duration.value!!
        driveTime = w.driveTime.value!!
        distance = w.distance.value!!
    }

}

class WorkTimeData: ViewModel() {
    val date = MutableLiveData<String>().apply { value =  getCurrentDate()}

    val employee = MutableLiveData<String>().apply { value =  storageHandler().configuration.employeeName}

    val duration = MutableLiveData<String>().apply { value =  "00:00"}

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
/*
    companion object {
        @BindingAdapter("textInt")
        @JvmStatic fun setTextInt(view: EditText, integerVal: Int) {
            Log.d("Arbeitsbericht.setTextInt", "${integerVal.toString()}")
            view.setText(integerVal.toString())
        }
        //@InverseMethod("setTextInt")
        @InverseBindingAdapter(attribute="textInt")
        @JvmStatic fun getTextInt(view: EditText): Int {
            Log.d("Arbeitsbericht.getTextInt", "${view.text.toString()}")
            var ret = 0
            try {
                ret = view.text.toString().toInt()
            } catch(e: NumberFormatException) {

            }
            return ret
        }
        @BindingAdapter(value=["app:textIntAttrChanged"])
        @JvmStatic fun setListener(view: TextInputEditText, listener: InverseBindingListener) {
            listener.onChange()
        }
    }
*/
    fun copyFromSerialized(w: WorkTimeDataSerialized) {
        date.value = w.date
        employee.value = w.employee
        duration.value = w.duration
        driveTime.value = w.driveTime
        distance.value = w.distance
    }
}

@Serializable
class WorkTimeContainerDataSerialized() {
    val items = mutableListOf<WorkTimeDataSerialized>()

    fun copyFromData(w: WorkTimeContainerData) {
        Log.d("Arbeitsbericht.WorkTimeContainerDataSerialized.copyFromData", "called")
        items.clear()
        for(i in 0 until w.items.size) {
            val item = WorkTimeDataSerialized()
            item.copyFromData(w.items[i])
            items.add(item)
        }
    }

}

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
}

@Serializable
class BillDataSerialized {
    var name: String = ""
    var street: String = ""
    var zip: String = ""
    var city: String = ""

    fun copyFromData(b: BillData) {
        name = b.name.value!!
        street = b.street.value!!
        zip = b.zip.value!!
        city = b.city.value!!
    }

}

class BillData() : ViewModel() {
    val name = MutableLiveData<String>().apply { value = ""}
    val street = MutableLiveData<String>().apply { value = ""}
    val zip = MutableLiveData<String>().apply { value = ""}
    val city = MutableLiveData<String>().apply { value = ""}

    fun copyFromSerialized(b: BillDataSerialized) {
        name.value = b.name
        street.value = b.street
        zip.value = b.zip
        city.value = b.city
    }

}

@Serializable
class ProjectDataSerialized {
    var name: String = ""
    var extra1: String = ""

    fun copyFromData(p: ProjectData) {
        name = p.name.value!!
        extra1 = p.extra1.value!!
    }

}

class ProjectData() : ViewModel() {
    val name = MutableLiveData<String>().apply { value = "" }
    val extra1 = MutableLiveData<String>().apply { value = "" }

    fun copyFromSerialized(p: ProjectDataSerialized) {
        name.value = p.name
        extra1.value = p.extra1
    }
}

@Serializable
private class ReportDataSerialized() {
    var id: Int = 0
    var create_date: String = "00:00"
    var change_date: String = "00:00"
    var project = ProjectDataSerialized()
    var bill = BillDataSerialized()
    val workTimeContainer = WorkTimeContainerDataSerialized()

    fun copyFromData(r: ReportData) {
        id = r.id.value!!
        create_date = r.create_date.value!!
        change_date = r.change_date.value!!
        project.copyFromData(r.project)
        bill.copyFromData(r.bill)
        workTimeContainer.copyFromData(r.workTimeContainer)
    }

}

class ReportData private constructor(val __id: Int = 0): ViewModel() {
    private val _id = MutableLiveData<Int>()
    val id: LiveData<Int>
        get() = _id

    private val _create_date = MutableLiveData<String>()
    val create_date: LiveData<String>
        get() = _create_date

    private val _change_date = MutableLiveData<String>()
    val change_date: LiveData<String>
        get() = _change_date

    var project = ProjectData()
    var bill = BillData()
    val workTimeContainer = WorkTimeContainerData()

    init {
        _id.value = __id
        _create_date.value = getCurrentDate()
        _change_date.value = _create_date.value
    }

    private fun copyFromSerialized(r: ReportDataSerialized) {
        _id.value = r.id
        _create_date.value = r.create_date
        _change_date.value = r.change_date
        project.copyFromSerialized(r.project)
        bill.copyFromSerialized(r.bill)
        workTimeContainer.copyFromSerialized(r.workTimeContainer)
    }

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
        _change_date.value = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2,'0') + "." +
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
            report.copyFromSerialized(serialized)
            return report
        }

        fun getJsonFromReport(r: ReportData): String {
            val serialized = ReportDataSerialized()
            serialized.copyFromData(r) // Copy from data object into serialized object
            val json: String = Json.stringify(ReportDataSerialized.serializer(), serialized)
            return json
        }

        fun createReport(id: Int): ReportData {
            return ReportData(id)
        }
    }
}
