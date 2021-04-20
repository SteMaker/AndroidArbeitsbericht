package com.stemaker.arbeitsbericht.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LumpSumContainerData(): ViewModel() {
    val items = mutableListOf<LumpSumData>()
    /* The list below won't go into the json / serialized data */
    private var _list = MutableLiveData<List<String>>().apply { value = configuration().lumpSums }
    val list: LiveData<List<String>>
        get() = _list
    fun updateLumpSums() {
        _list.value = configuration().lumpSums
    }
    val visibility = MutableLiveData<Boolean>().apply { value = false }

    fun copyFromSerialized(l: LumpSumContainerDataSerialized) {
        visibility.value = l.visibility
        items.clear()
        for(i in 0 until l.items.size) {
            val item = LumpSumData()
            item.copyFromSerialized(l.items[i])
            items.add(item)
        }
    }

    fun copyFromDb(w: LumpSumContainerDb) {
        visibility.value = w.lVisibility
        items.clear()
        for(element in w.lItems) {
            val item = LumpSumData()
            item.copyFromDb(element)
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

class LumpSumData(): ViewModel() {
    var item = MutableLiveData<String>().apply { value = "" }
    var amount = MutableLiveData<Int>().apply { value = 0 }
    var comment = MutableLiveData<String>().apply { value = "" }

    fun copyFromSerialized(l: LumpSumDataSerialized) {
        item.value = l.item
        amount.value = l.amount
        comment.value = l.comment
    }

    fun copyFromDb(l: LumpSumContainerDb.LumpSumDb) {
        item.value = l.lItem
        amount.value = l.lAmount
        comment.value = l.lComment
    }
}
