package com.stemaker.arbeitsbericht.view_models

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stemaker.arbeitsbericht.data.report.WorkItemData
import com.stemaker.arbeitsbericht.data.report.WorkItemContainerData

class WorkItemContainerViewModel(
    private val lifecycleOwner: LifecycleOwner,
    private val workItemContainer: WorkItemContainerData,
    val definedWorkItems: LiveData<Set<String>>)
    : ViewModel(),
    Iterable<WorkItemViewModel>
{
    val visibility = workItemContainer.visibility

    private val itemViewModels = mutableListOf<WorkItemViewModel>()
    init {
        for(item in workItemContainer.items) {
            itemViewModels.add(WorkItemViewModel(lifecycleOwner, item))
        }
    }
    fun addWorkItem(): WorkItemViewModel {
        val wi = workItemContainer.addWorkItem()
        return WorkItemViewModel(lifecycleOwner, wi)
    }
    fun removeWorkItem(w: WorkItemViewModel) {
        workItemContainer.removeWorkItem(w.getData())
    }

    override fun iterator(): Iterator<WorkItemViewModel> {
        return object: Iterator<WorkItemViewModel> {
            var idx = 0
            override fun hasNext(): Boolean {
                return idx < itemViewModels.size
            }
            override fun next(): WorkItemViewModel {
                val r = itemViewModels[idx]
                idx++
                return r
            }
        }
    }
}

class WorkItemContainerViewModelFactory(
    private val lifecycleOwner: LifecycleOwner,
    private val workItemContainer: WorkItemContainerData,
    private val definedWorkItems: LiveData<Set<String>>
)
    : ViewModelProvider.Factory
{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkItemContainerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkItemContainerViewModel(lifecycleOwner, workItemContainer, definedWorkItems) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
class WorkItemViewModel(private val lifecycleOwner: LifecycleOwner, private val workItem: WorkItemData): ViewModel() {
    val item = workItem.item
    // Use this only within the ContainerViewModel above
    fun getData(): WorkItemData = workItem
}
