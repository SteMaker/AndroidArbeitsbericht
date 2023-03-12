package com.stemaker.arbeitsbericht.view_models

import androidx.lifecycle.*
import com.stemaker.arbeitsbericht.data.preferences.ConfigElement
import com.stemaker.arbeitsbericht.data.report.WorkItemData
import com.stemaker.arbeitsbericht.data.report.WorkItemContainerData

/* Dictionary strategy: Keep a local copy that is initialized with the already defined
   dictionary. Fill the local dictionary whenever focus on a respective text element is lost.
   When saving the report add the still existing work items to the prefs dictionary.
 */
class WorkItemContainerViewModel(
    private val lifecycleOwner: LifecycleOwner,
    private val workItemContainer: WorkItemContainerData,
    val definedWorkItems: ConfigElement<Set<String>>
)
    : ViewModel(),
    Iterable<WorkItemViewModel>
{
    val visibility = workItemContainer.visibility
    val workItemDictionary = MutableLiveData<MutableSet<String>>()

    private val itemViewModels = mutableListOf<WorkItemViewModel>()
    init {
        for(item in workItemContainer.items) {
            itemViewModels.add(WorkItemViewModel(lifecycleOwner, item))
        }
        workItemDictionary.value = definedWorkItems.value.toMutableSet()
    }
    fun addWorkItem(): WorkItemViewModel {
        val wi = workItemContainer.addWorkItem()
        val vm = WorkItemViewModel(lifecycleOwner, wi)
        itemViewModels.add(vm)
        return vm
    }
    fun removeWorkItem(w: WorkItemViewModel) {
        itemViewModels.remove(w)
        workItemContainer.removeWorkItem(w.getData())
    }

    fun addToDictionary(dictEntry: String) {
        workItemDictionary.value?.add(dictEntry)
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
    private val definedWorkItems: ConfigElement<Set<String>>
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
