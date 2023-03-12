package com.stemaker.arbeitsbericht.view_models

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.base.DataElement
import com.stemaker.arbeitsbericht.data.base.DataModificationEvent
import com.stemaker.arbeitsbericht.data.report.*

private const val TAG = "WorkTimeViewModel"

interface WorkTimeInteractionFragment {
    fun onReorder()
}

class WorkTimeContainerViewModel(
    private val lifecycleOwner: LifecycleOwner,
    private val workTimeInteractionFragment: WorkTimeInteractionFragment,
    private val workTimeContainer: WorkTimeContainerData,
    private val defaultValues: DefaultValues)
    : ViewModel(),
    Iterable<WorkTimeViewModel>
{
    val visibility = workTimeContainer.visibility
    private val itemViewModels = mutableListOf<WorkTimeViewModel>()
    init {
        for(item in workTimeContainer.items) {
            itemViewModels.add(WorkTimeViewModel(lifecycleOwner, item))
        }
        val containerObserver = Observer<DataModificationEvent> { event ->
            if(event.type == DataModificationEvent.Type.CONTAINER_REORDER) {
                itemViewModels.clear()
                for(item in workTimeContainer.items) {
                    itemViewModels.add(WorkTimeViewModel(lifecycleOwner, item))
                }
                workTimeInteractionFragment.onReorder()
            }
        }
        workTimeContainer.containerModificationEvent.observe(lifecycleOwner, containerObserver)
    }

    fun addWorkTime(ctx: Context): Pair<String?, WorkTimeViewModel> {
        val wt = workTimeContainer.addWorkTime(defaultValues)
        val viewModel = WorkTimeViewModel(lifecycleOwner, wt)
        itemViewModels.add(viewModel)
        val infoText = when {
            defaultValues.useDefaultDriveTime && defaultValues.useDefaultDistance ->
                ctx.getString(R.string.preset_used_drive_distance, defaultValues.defaultDriveTime, defaultValues.defaultDistance)
            defaultValues.useDefaultDriveTime ->
                ctx.getString(R.string.preset_used_drive,defaultValues.defaultDriveTime)
            defaultValues.useDefaultDistance ->
                ctx.getString(R.string.preset_used_distance,defaultValues.defaultDistance)
            else -> null
        }
        return Pair(infoText, viewModel)
    }
    fun removeWorkTime(w: WorkTimeViewModel) {
        itemViewModels.remove(w)
        workTimeContainer.removeWorkTime(w.getData())
    }
    fun cloneWorkTime(origin: WorkTimeViewModel): WorkTimeViewModel {
        val wt = workTimeContainer.addWorkTime(origin.getData())
        return WorkTimeViewModel(lifecycleOwner, wt)
    }

    fun sortByDate() {
        workTimeContainer.sortByDate()

    }

    override fun iterator(): Iterator<WorkTimeViewModel> {
        return object: Iterator<WorkTimeViewModel> {
            var idx = 0
            override fun hasNext(): Boolean {
                return idx < itemViewModels.size
            }
            override fun next(): WorkTimeViewModel {
                val r = itemViewModels[idx]
                idx++
                return r
            }
        }
    }
}

class WorkTimeContainerViewModelFactory(
    private val lifecycleOwner: LifecycleOwner,
    private val workTimeInteractionFragment: WorkTimeInteractionFragment,
    private val workTimeContainer: WorkTimeContainerData,
    private val defaultValues: DefaultValues)
    : ViewModelProvider.Factory
{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkTimeContainerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkTimeContainerViewModel(lifecycleOwner, workTimeInteractionFragment, workTimeContainer, defaultValues) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
class WorkTimeViewModel(private val lifecycleOwner: LifecycleOwner, private val workTime: WorkTimeData): ViewModel() {
    val date = workTime.date
    val startTime = workTime.startTime
    val endTime = workTime.endTime
    val pauseDuration = workTime.pauseDuration
    val driveTime = workTime.driveTime
    val distance = workTime.distance
    val workDuration = workTime.workDuration

    val employees = mutableListOf<MutableLiveData<String>>()
    init {
        for(e in workTime.employees) {
            employees.add(e)
        }
    }
    fun addEmployee(): MutableLiveData<String> {
        val e = workTime.addEmployee()
        employees.add(e)
        return e
    }
    fun removeEmployee(e: MutableLiveData<String>) {
        employees.remove(e)
        if(e is DataElement<String>) {
            workTime.removeEmployee(e)
        } else {
            Log.e(TAG, "Trying to remove employee which is not DataElement")
        }
    }

    // Use this only within the ContainerViewModel above
    fun getData(): WorkTimeData = workTime
}