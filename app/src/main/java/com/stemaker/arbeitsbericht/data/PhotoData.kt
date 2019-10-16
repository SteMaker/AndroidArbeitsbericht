package com.stemaker.arbeitsbericht.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PhotoContainerData(): ViewModel() {
    val items = mutableListOf<PhotoData>()

    fun copyFromSerialized(p: PhotoContainerDataSerialized) {
        items.clear()
        for(i in 0 until p.items.size) {
            val item = PhotoData()
            item.copyFromSerialized(p.items[i])
            items.add(item)
        }
    }

    fun addPhoto(): PhotoData {
        val p = PhotoData()
        items.add(p)
        return p
    }

    fun removePhoto(p: PhotoData) {
        items.remove(p)
    }
}

class PhotoData: ViewModel() {
    val file = MutableLiveData<String>().apply { value =  "" }
    val description = MutableLiveData<String>().apply { value = "" }

    fun copyFromSerialized(p: PhotoDataSerialized) {
        file.value = p.file
        description.value = p.description
    }
}