package com.stemaker.arbeitsbericht.data.report

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stemaker.arbeitsbericht.storageHandler

class MaterialContainerData {
    val items = mutableListOf<MaterialData>()
    /* The list below won't go into the json / serialized data */
    private var _dictionary = MutableLiveData<Set<String>>().apply { value = storageHandler().materialDictionary }
    val dictionary: LiveData<Set<String>>
        get() = _dictionary
    val visibility = MutableLiveData<Boolean>().apply { value = false }
    private var _units = MutableLiveData<Set<String>>().apply { value = setOf("Stück", "Meter", "Packung", "Liter", "VPE") }
    val units: LiveData<Set<String>>
        get() = _units

    fun isAnyMaterialUnitSet(): Boolean {
        for(item in items) {
            if(item.unit.value!! != "") return true
        }
        return false
    }

    fun copyFromSerialized(m: MaterialContainerDataSerialized) {
        visibility.value = m.visibility
        items.clear()
        for(i in 0 until m.items.size) {
            val item = MaterialData()
            item.copyFromSerialized(m.items[i])
            items.add(item)
        }
    }

    fun copyFromDb(w: MaterialContainerDb) {
        visibility.value = w.mVisibility
        items.clear()
        for(element in w.mItems) {
            val item = MaterialData()
            item.copyFromDb(element)
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

class MaterialData {
    var item = MutableLiveData<String>().apply { value = "" }
    var amount = MutableLiveData<Float>().apply { value = 0f }
    var unit = MutableLiveData<String>().apply { value = "" }

    fun copyFromSerialized(m: MaterialDataSerialized) {
        item.value = m.item
        amount.value = m.amount
        unit.value = m.unit
    }
    fun copyFromDb(m: MaterialContainerDb.MaterialDb) {
        item.value = m.mItem
        amount.value = m.mAmount
        unit.value = m.mUnit
    }
}
