package com.stemaker.arbeitsbericht.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stemaker.arbeitsbericht.storageHandler

class MaterialContainerData(): ViewModel() {
    val items = mutableListOf<MaterialData>()
    /* The list below won't go into the json / serialized data */
    private var _dictionary = MutableLiveData<Set<String>>().apply { value = storageHandler().materialDictionary }
    val dictionary: LiveData<Set<String>>
        get() = _dictionary

    fun copyFromSerialized(m: MaterialContainerDataSerialized) {
        items.clear()
        for(i in 0 until m.items.size) {
            val item = MaterialData()
            item.copyFromSerialized(m.items[i])
            items.add(item)
        }
    }

    fun addMaterial(): MaterialData {
        val m = MaterialData()
        items.add(m)
        return m
    }

    fun removeMaterial(m: MaterialData) {
        items.remove(m)
    }
}

class MaterialData(): ViewModel() {
    var item = MutableLiveData<String>().apply { value = "" }
    var amount = MutableLiveData<Int>().apply { value = 0 }

    fun copyFromSerialized(m: MaterialDataSerialized) {
        item.value = m.item
        amount.value = m.amount
    }
}
