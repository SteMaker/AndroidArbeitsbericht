package com.stemaker.arbeitsbericht

import java.util.*
import kotlinx.serialization.*

@Serializable
class WorkTime(var date: String = "", var employee: String = StorageHandler.configuration.employeeName,
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
class Material(var item: String = "", var amount: Int = 0) {
}

@Serializable
class Report(val id: Int) {
    var client_name: String = ""
    var client_extra1: String = ""
    var create_date: String
    var change_date: String
    var bill_address_name: String = ""
    var bill_address_street: String = ""
    var bill_address_zip: String = ""
    var bill_address_city: String = ""
    var work_times = mutableListOf<WorkTime>()
    var work_items = mutableListOf<WorkItem>()
    var material = mutableListOf<Material>()
    var client_signature: String = ""
    var employee_signature: String = ""

    init {
        val d = Date()
        val cal = Calendar.getInstance()
        cal.time = d
        create_date = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2,'0') + "." +
                (cal.get(Calendar.MONTH)+1).toString().padStart(2,'0') + "." +
                cal.get(Calendar.YEAR).toString().padStart(4,'0')
        change_date = create_date
    }

    fun updateLastChangeDate() {
        val d = Date()
        val cal = Calendar.getInstance()
        cal.time = d
        change_date = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2,'0') + "." +
                (cal.get(Calendar.MONTH)+1).toString().padStart(2,'0') + "." +
                cal.get(Calendar.YEAR).toString().padStart(4,'0')

    }
}
