package com.stemaker.arbeitsbericht.data.report

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stemaker.arbeitsbericht.data.base.*
import com.stemaker.arbeitsbericht.storageHandler

const val MATERIAL_CONTAINER_VISIBILITY = "macVis"
const val MATERIAL_CONTAINER = "mac"
class MaterialContainerData: DataContainer<MaterialData>(MATERIAL_CONTAINER) {
    val visibility = DataElement<Boolean>(MATERIAL_CONTAINER_VISIBILITY) { false }

    /* The list below won't go into the json / serialized data */
    private var _dictionary = MutableLiveData<Set<String>>().apply { value = storageHandler().materialDictionary }
    val dictionary: LiveData<Set<String>>
        get() = _dictionary
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
        clear()
        for(i in 0 until m.items.size) {
            val item = MaterialData()
            item.copyFromSerialized(m.items[i])
            add(item)
        }
    }

    fun copyFromDb(w: MaterialContainerDb) {
        visibility.value = w.mVisibility
        clear()
        for(element in w.mItems) {
            val item = MaterialData()
            item.copyFromDb(element)
            add(item)
        }
    }

    fun copy(w: MaterialContainerData) {
        visibility.copy(w.visibility)
        clear()
        for(element in w.items) {
            val item = MaterialData()
            item.copy(element)
            add(item)
        }
    }

    fun addMaterial(): MaterialData {
        val m = MaterialData()
        add(m)
        return m
    }

    fun removeMaterial(m: MaterialData) {
        remove(m)
    }
}

const val MATERIAL_DATA = "maData"
const val MATERIAL_ITEM = "maItem"
const val MATERIAL_AMOUNT = "maAmount"
const val MATERIAL_UNIT = "maUnit"
class MaterialData: DataObject(MATERIAL_DATA) {

    val item = DataElement<String>(MATERIAL_ITEM) { "" }
    val amount = DataElement<Float>(MATERIAL_AMOUNT) { 0f }
    val unit = DataElement<String>(MATERIAL_UNIT) { "" }

    override val elements = listOf<DataBasicIf>(
        item, amount, unit
    )

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

    fun copy(m: MaterialData) {
        item.copy(m.item)
        amount.copy(m.amount)
        unit.copy(m.unit)
    }
}
