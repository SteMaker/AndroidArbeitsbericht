package com.stemaker.arbeitsbericht.helpers

import androidx.lifecycle.*
import com.stemaker.arbeitsbericht.data.configuration.configuration
import com.stemaker.arbeitsbericht.data.report.ReportData

open class ReportFilter() {
    var projectName = ""
        set(value) {
            val u = field != value
            field = value
            if(u) update()
        }
    var projectExtra = ""
        set(value) {
            val u = field != value
            field = value
            if(u) update()
        }
    val remainingStates = mutableSetOf<Int>()

    var blockUpdate = false

    var inWork = true
        get() = remainingStates.contains(ReportData.ReportState.toInt(ReportData.ReportState.IN_WORK))
        set(v: Boolean) {
            val u = field != v
            field = v
            if (v) remainingStates.add(ReportData.ReportState.toInt(ReportData.ReportState.IN_WORK))
            else remainingStates.remove(ReportData.ReportState.toInt(ReportData.ReportState.IN_WORK))
            if(u) update()
        }
    var onHold = true
        get() = remainingStates.contains(ReportData.ReportState.toInt(ReportData.ReportState.ON_HOLD))
        set(v: Boolean) {
            val u = field != v
            field = v
            if (v) remainingStates.add(ReportData.ReportState.toInt(ReportData.ReportState.ON_HOLD))
            else remainingStates.remove(ReportData.ReportState.toInt(ReportData.ReportState.ON_HOLD))
            if(u) update()
        }
    var done = true
        get() = remainingStates.contains(ReportData.ReportState.toInt(ReportData.ReportState.DONE))
        set(v: Boolean) {
            val u = field != v
            field = v
            if (v) remainingStates.add(ReportData.ReportState.toInt(ReportData.ReportState.DONE))
            else remainingStates.remove(ReportData.ReportState.toInt(ReportData.ReportState.DONE))
            if(u) update()
        }
    var archived = false
        get() = remainingStates.contains(ReportData.ReportState.toInt(ReportData.ReportState.ARCHIVED))
        set(v: Boolean) {
            val u = field != v
            field = v
            if (v) remainingStates.add(ReportData.ReportState.toInt(ReportData.ReportState.ARCHIVED))
            else remainingStates.remove(ReportData.ReportState.toInt(ReportData.ReportState.ARCHIVED))
            if(u) update()
        }

    private val _live = MutableLiveData<Unit>()
    val live: LiveData<Unit>
        get() = _live

    open fun isFiltered(r: ReportData): Boolean {
        if(!remainingStates.contains(ReportData.ReportState.toInt(r.state.value!!)))
            return true
        if(projectName != "" && !r.project.name.value!!.contains(projectName))
            return true
        if(projectExtra != "" && !r.project.extra1.value!!.contains(projectExtra))
            return true
        return false
    }

    fun update() {
        if(!blockUpdate)
            _live.value = Unit
    }

    fun fromStore(pN: String, pE: String, states: Int) {
        blockUpdate = true
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
        blockUpdate = true
        update()
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

    companion object {
        val noneFilter = object: ReportFilter() {
            init {
                inWork = true
                onHold = true
                done = true
                archived = true
            }
        }
    }
}