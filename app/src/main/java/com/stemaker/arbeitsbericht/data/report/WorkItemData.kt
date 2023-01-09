package com.stemaker.arbeitsbericht.data.report

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stemaker.arbeitsbericht.data.base.*
import com.stemaker.arbeitsbericht.storageHandler

const val WORK_ITEM_CONTAINER_VISIBILITY = "wtcVis"
const val WORK_ITEM_CONTAINER = "wic"
class WorkItemContainerData: DataContainer<WorkItemData>(WORK_ITEM_CONTAINER) {
    val visibility = DataElement<Boolean>(WORK_ITEM_CONTAINER_VISIBILITY) { false }

    /* The list below won't go into the json / serialized data */
    private var _dictionary = MutableLiveData<Set<String>>().apply { value = storageHandler().workItemDictionary }
    val dictionary: LiveData<Set<String>>
        get() = _dictionary

    fun copyFromSerialized(w: WorkItemContainerDataSerialized) {
        visibility.value = w.visibility
        clear()
        for(i in 0 until w.items.size) {
            val item = WorkItemData()
            item.copyFromSerialized(w.items[i])
            add(item)
        }
    }

    fun copyFromDb(w: WorkItemContainerDb) {
        visibility.value = w.wiVisibility
        clear()
        for(element in w.wiItems) {
            val item = WorkItemData()
            item.copyFromDb(element)
            add(item)
        }
    }

    fun copy(w: WorkItemContainerData) {
        visibility.value = w.visibility.value
        clear()
        for(element in w.items) {
            val item = WorkItemData()
            item.copy(element)
            add(item)
        }
    }
    fun addWorkItem(): WorkItemData {
        val wi = WorkItemData()
        add(wi)
        return wi
    }

    fun removeWorkItem(wi: WorkItemData) {
        remove(wi)
    }
}

const val WORK_ITEM_DATA = "workItemData"
const val WORK_ITEM_ITEM = "wiItem"
class WorkItemData: DataObject(WORK_ITEM_DATA)  {
    val item = DataElement<String>(WORK_ITEM_ITEM) { "" }

    override val elements = listOf<DataBasicIf>(
        item
    )

    fun copyFromSerialized(w: WorkItemDataSerialized) {
        item.value = w.item
    }
    fun copyFromDb(w: WorkItemContainerDb.WorkItemDb) {
        item.value = w.wiItem
    }
    fun copy(w: WorkItemData) {
        item.copy(w.item)
    }
}