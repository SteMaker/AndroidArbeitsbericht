package com.stemaker.arbeitsbericht.data.hoursreport

import kotlinx.serialization.Serializable

private const val vers = 100
@Serializable
class HoursReportDataSerialized() {
    var formatVersion: Int = 0
    var id: Long = 0
    var date: String = "00:00"
    var project: String = ""
    var projectNr: String = ""
    var workItems = mutableListOf("")
    var startTime: String = "00:00"
    var endTime: String = "00:00"

    fun copyFromData(r: HoursReportData) {
        formatVersion = vers
        id = r.id
        date = r.date.value!!
        project = r.project.value!!
        projectNr = r.projectNr.value!!
        workItems.clear()
        for (wiData in r.workItems)
            workItems.add(wiData.value!!)
        startTime = r.startTime.value!!
        endTime = r.endTime.value!!
    }
}