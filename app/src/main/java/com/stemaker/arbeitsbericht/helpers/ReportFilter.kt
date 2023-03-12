package com.stemaker.arbeitsbericht.helpers

import android.util.Log
import androidx.lifecycle.*
import com.stemaker.arbeitsbericht.data.preferences.ConfigElement
import com.stemaker.arbeitsbericht.data.report.ReportData

private const val TAG = "ReportFilter"
// The 3 parameters are the globally stored ones in the preferences
// There is no other internal storage here, it is all directly derived from
// and written to those 3 pars
open class ReportFilter(
    private val _projectName: ConfigElement<String>,
    private val _projectExtra: ConfigElement<String>,
    private val _filterStates: ConfigElement<Int>)
{
    var projectName: String
        get() = _projectName.value
        set(value) {
            if(value != _projectName.value) {
                _projectName.value = value
                update()
            }
        }
    var projectExtra: String
        get() = _projectExtra.value
        set(value) {
            if(value != _projectExtra.value) {
                _projectExtra.value = value
                update()
            }
        }
    private var filterStates
        get() = _filterStates.value
        set(value) {
            _filterStates.value = value
        }

    val remainingStates: MutableSet<Int>
        get() {
            val theSet = mutableSetOf<Int>()
            val states = filterStates
            for(i in 0..3) {
                if ((states and (1 shl i)) != 0) {
                    theSet.add(i)
                }
            }
            return theSet
        }

    var blockUpdate: Boolean = false
        set(value) {
            field = value
            if(!value) {
                update()
            }
        }

    private fun isStateSet(state: ReportData.ReportState): Boolean {
        return (filterStates and (1 shl state.ordinal) != 0)
    }
    private fun setMaskFromState(state: ReportData.ReportState): Int {
        val ret = (1 shl state.ordinal)
        Log.d(TAG, "setMaskFromState($state) -> $ret")
        return ret
    }
    private fun clearMaskFromState(state: ReportData.ReportState): Int {
        val ret = ((1 shl (ReportData.ReportState.max+1))-1) and (1 shl state.ordinal).inv()
        Log.d(TAG, "clearMaskFromState($state) -> $ret")
        return ret
    }
    private fun setState(value: Boolean, state: ReportData.ReportState) {
        val currentlySet = isStateSet(state)
        if(value && !currentlySet) {
            Log.d(TAG, "pre setState($value, $state), filterStates = $filterStates")
            filterStates = filterStates or setMaskFromState(state)
            Log.d(TAG, "post setState($value, $state), filterStates = $filterStates")
            update()
        } else if(!value && currentlySet) {
            Log.d(TAG, "pre setState($value, $state), filterStates = $filterStates")
            filterStates = filterStates and clearMaskFromState(state)
            Log.d(TAG, "post setState($value, $state), filterStates = $filterStates")
            update()
        }

    }

    var inWork: Boolean
        get() = isStateSet(ReportData.ReportState.IN_WORK)
        set(value) {
            setState(value, ReportData.ReportState.IN_WORK)
        }

    var onHold: Boolean
        get() = isStateSet(ReportData.ReportState.ON_HOLD)
        set(value) {
            setState(value, ReportData.ReportState.ON_HOLD)
        }

    var done: Boolean
        get() = isStateSet(ReportData.ReportState.DONE)
        set(value) {
            setState(value, ReportData.ReportState.DONE)
        }

    var archived: Boolean
        get() = isStateSet(ReportData.ReportState.ARCHIVED)
        set(value) {
            setState(value, ReportData.ReportState.ARCHIVED)
        }

    // This is updated whenever the filter changes, except when blockUpdate == false
    private val _live = MutableLiveData<Unit>()
    val live: LiveData<Unit>
        get() = _live

    private fun update() {
        if(!blockUpdate)
            _live.value = Unit
    }
}