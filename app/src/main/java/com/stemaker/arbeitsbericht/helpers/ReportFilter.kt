package com.stemaker.arbeitsbericht.helpers

import androidx.databinding.Observable
import androidx.lifecycle.*
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.data.configuration
import com.stemaker.arbeitsbericht.data.report.ReportData
import com.stemaker.arbeitsbericht.data.report.ReportListChangeEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport

class ReportFilterChangeEvent(): PropertyChangeEvent() {
}

class ReportFilter() {
    var projectName = ""
    var projectExtra = ""
    val remainingStates = mutableSetOf<Int>()

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
    private val propCS = PropertyChangeSupport(this)
    fun addReportFilterObserver(listener: PropertyChangeListener) {
        propCS.addPropertyChangeListener(listener)
    }
    fun removeReportFilterObserver(listener: PropertyChangeListener) {
        propCS.removePropertyChangeListener(listener)
    }

    fun isFiltered(r: ReportData): Boolean {
        if(!remainingStates.contains(ReportData.ReportState.toInt(r.state.value!!)))
            return true
        if(projectName != "" && !r.project.name.value!!.contains(projectName))
            return true
        if(projectExtra != "" && !r.project.extra1.value!!.contains(projectExtra))
            return true
        return false
    }

    fun update() {
        propCS.firePropertyChange(ReportFilterChangeEvent())
    }

    fun fromStore(pN: String, pE: String, states: Int) {
        projectName = pN
        projectExtra = pE
        if(states and (1 shl ReportData.ReportState.toInt(ReportData.ReportState.IN_WORK)) != 0)
            inWork = true
        if(states and (1 shl ReportData.ReportState.toInt(ReportData.ReportState.ON_HOLD)) != 0)
            onHold = true
        if(states and (1 shl ReportData.ReportState.toInt(ReportData.ReportState.DONE)) != 0)
            done = true
        if(states and (1 shl ReportData.ReportState.toInt(ReportData.ReportState.ARCHIVED)) != 0)
            archived = true
    }

    fun save() {
        var filterStates = 0
        for(i in remainingStates) {
            filterStates = filterStates or (1 shl i)
        }
        configuration().filterProjectName = projectName
        configuration().filterProjectExtra = projectExtra
        configuration().filterStates = filterStates
        configuration().save()
    }
}