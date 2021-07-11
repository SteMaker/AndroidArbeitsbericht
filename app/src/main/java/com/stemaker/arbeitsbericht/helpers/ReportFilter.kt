package com.stemaker.arbeitsbericht.helpers

import androidx.databinding.Observable
import androidx.lifecycle.*
import com.stemaker.arbeitsbericht.data.ReportData

class ReportFilter(): Observable, ViewModel() {
    var projectName = ""
    var projectExtra = ""
    val remainingStates = mutableSetOf<Int>(
        ReportData.ReportState.toInt(ReportData.ReportState.IN_WORK),
        ReportData.ReportState.toInt(ReportData.ReportState.ON_HOLD),
        ReportData.ReportState.toInt(ReportData.ReportState.DONE))

    var inWork
        get() = remainingStates.contains(ReportData.ReportState.toInt(ReportData.ReportState.IN_WORK))?:true
        set(v: Boolean) {
            if (v) remainingStates.add(ReportData.ReportState.toInt(ReportData.ReportState.IN_WORK))
            else remainingStates.remove(ReportData.ReportState.toInt(ReportData.ReportState.IN_WORK))
        }
    var onHold
        get() = remainingStates.contains(ReportData.ReportState.toInt(ReportData.ReportState.ON_HOLD))?:true
        set(v: Boolean) {
            if (v) remainingStates.add(ReportData.ReportState.toInt(ReportData.ReportState.ON_HOLD))
            else remainingStates.remove(ReportData.ReportState.toInt(ReportData.ReportState.ON_HOLD))
        }
    var done
        get() = remainingStates.contains(ReportData.ReportState.toInt(ReportData.ReportState.DONE))?:true
        set(v: Boolean) {
            if (v) remainingStates.add(ReportData.ReportState.toInt(ReportData.ReportState.DONE))
            else remainingStates.remove(ReportData.ReportState.toInt(ReportData.ReportState.DONE))
        }
    var archived
        get() = remainingStates.contains(ReportData.ReportState.toInt(ReportData.ReportState.ARCHIVED))?:true
        set(v: Boolean) {
            if (v) remainingStates.add(ReportData.ReportState.toInt(ReportData.ReportState.ARCHIVED))
            else remainingStates.remove(ReportData.ReportState.toInt(ReportData.ReportState.ARCHIVED))
        }

    // observers
    private val observers = mutableListOf<Observable.OnPropertyChangedCallback>()

    fun isFiltered(r: ReportData): Boolean {
        if(!remainingStates.contains(ReportData.ReportState.toInt(r.state.value!!)))
            return true
        if(projectName != "" && !r.project.name.value!!.contains(projectName))
            return true
        if(projectExtra != "" && !r.project.extra1.value!!.contains(projectExtra))
            return true
        return false
    }

    override fun addOnPropertyChangedCallback(observer: Observable.OnPropertyChangedCallback?) {
        observer?.let { observers.add(it) }
    }

    override fun removeOnPropertyChangedCallback(observer: Observable.OnPropertyChangedCallback?) {
        observer?.let { observers.remove(it) }
    }

    fun update() {
        observers.forEach {
            it.onPropertyChanged(this, 0)
        }
    }
}