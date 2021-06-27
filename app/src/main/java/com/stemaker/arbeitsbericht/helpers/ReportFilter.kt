package com.stemaker.arbeitsbericht.helpers

import androidx.databinding.BaseObservable
import com.stemaker.arbeitsbericht.data.ReportData

class ReportFilter: BaseObservable() {
    var remainingStates = mutableSetOf<Int>(
        ReportData.ReportState.toInt(ReportData.ReportState.IN_WORK),
        ReportData.ReportState.toInt(ReportData.ReportState.ON_HOLD),
        ReportData.ReportState.toInt(ReportData.ReportState.DONE)
    )
    val projectName = ""
    var inWork
        get() = remainingStates.contains(ReportData.ReportState.toInt(ReportData.ReportState.IN_WORK))
        set(v: Boolean) {
            if (v) remainingStates.add(ReportData.ReportState.toInt(ReportData.ReportState.IN_WORK))
            else remainingStates.remove(ReportData.ReportState.toInt(ReportData.ReportState.IN_WORK))
        }
    var onHold
        get() = remainingStates.contains(ReportData.ReportState.toInt(ReportData.ReportState.ON_HOLD))
        set(v: Boolean) {
            if (v) remainingStates.add(ReportData.ReportState.toInt(ReportData.ReportState.ON_HOLD))
            else remainingStates.remove(ReportData.ReportState.toInt(ReportData.ReportState.ON_HOLD))
        }
    var done
        get() = remainingStates.contains(ReportData.ReportState.toInt(ReportData.ReportState.DONE))
        set(v: Boolean) {
            if (v) remainingStates.add(ReportData.ReportState.toInt(ReportData.ReportState.DONE))
            else remainingStates.remove(ReportData.ReportState.toInt(ReportData.ReportState.DONE))
        }
    var archived
        get() = remainingStates.contains(ReportData.ReportState.toInt(ReportData.ReportState.ARCHIVED))
        set(v: Boolean) {
            if (v) remainingStates.add(ReportData.ReportState.toInt(ReportData.ReportState.ARCHIVED))
            else remainingStates.remove(ReportData.ReportState.toInt(ReportData.ReportState.ARCHIVED))
        }
    fun isRemaining(r: ReportData): Boolean {
        val state = remainingStates.contains(ReportData.ReportState.toInt(r.state.value!!))
        val projectName = when (r.project.name.value) {
            "" -> true
            else -> r.project.name.value?.contains(projectName) ?: let {true}
        }
        return state && projectName
    }

    fun isFiltered(r:ReportData): Boolean = !isRemaining(r)

    fun updateDone() {
        notifyChange()
    }
}