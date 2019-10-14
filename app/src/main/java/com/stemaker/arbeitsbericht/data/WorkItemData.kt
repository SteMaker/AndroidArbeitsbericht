package com.stemaker.arbeitsbericht.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WorkItemContainerData(): ViewModel() {
    val items = mutableListOf<WorkItemData>()

    fun copyFromSerialized(w: WorkItemContainerDataSerialized) {
        items.clear()
        for(i in 0 until w.items.size) {
            val item = WorkItemData()
            item.copyFromSerialized(w.items[i])
            items.add(item)
        }
    }

    fun addWorkItem(): WorkItemData {
        val wi = WorkItemData()
        items.add(wi)
        return wi
    }

    fun removeWorkItem(wi: WorkItemData) {
        items.remove(wi)
    }
}

class WorkItemData: ViewModel() {
    val item = MutableLiveData<String>().apply {value =  ""}

    fun copyFromSerialized(w: WorkItemDataSerialized) {
        item.value = w.item
    }
}