package com.stemaker.arbeitsbericht.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stemaker.arbeitsbericht.storageHandler

class LumpSumContainerData(): ViewModel() {
    val items = mutableListOf<LumpSumData>()
    /* The list below won't go into the json / serialized data */
    private var _list = MutableLiveData<List<String>>().apply { value = storageHandler().configuration.lumpSums }
    val list: LiveData<List<String>>
        get() = _list

    fun copyFromSerialized(l: LumpSumContainerDataSerialized) {
        items.clear()
        for(i in 0 until l.items.size) {
            val item = LumpSumData()
            item.copyFromSerialized(l.items[i])
            items.add(item)
        }
    }

    fun addLumpSum(): LumpSumData {
        val l = LumpSumData()
        items.add(l)
        return l
    }

    fun removeLumpSum(l: LumpSumData) {
        items.remove(l)
    }
}

class LumpSumData() {
    var item = MutableLiveData<String>().apply { value = "" }

    fun copyFromSerialized(l: LumpSumDataSerialized) {
        item.value = l.item
    }
}