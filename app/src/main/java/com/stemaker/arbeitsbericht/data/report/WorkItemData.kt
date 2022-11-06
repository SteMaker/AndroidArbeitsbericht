package com.stemaker.arbeitsbericht.data.report

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stemaker.arbeitsbericht.storageHandler

class WorkItemContainerData {
    val items = mutableListOf<WorkItemData>()
    /* The list below won't go into the json / serialized data */
    private var _dictionary = MutableLiveData<Set<String>>().apply { value = storageHandler().workItemDictionary }
    val dictionary: LiveData<Set<String>>
        get() = _dictionary
    val visibility = MutableLiveData<Boolean>().apply { value = false }

    fun copyFromSerialized(w: WorkItemContainerDataSerialized) {
        visibility.value = w.visibility
        items.clear()
        for(i in 0 until w.items.size) {
            val item = WorkItemData()
            item.copyFromSerialized(w.items[i])
            items.add(item)
        }
    }

    fun copyFromDb(w: WorkItemContainerDb) {
        visibility.value = w.wiVisibility
        items.clear()
        for(element in w.wiItems) {
            val item = WorkItemData()
            item.copyFromDb(element)
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

class WorkItemData {
    val item = MutableLiveData<String>().apply {value =  ""}

    fun copyFromSerialized(w: WorkItemDataSerialized) {
        item.value = w.item
    }
    fun copyFromDb(w: WorkItemContainerDb.WorkItemDb) {
        item.value = w.wiItem
    }
}