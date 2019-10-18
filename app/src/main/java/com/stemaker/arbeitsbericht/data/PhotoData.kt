package com.stemaker.arbeitsbericht.data

import android.util.Log
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
        Log.d("Arbeitsbericht.debug", "Adding photo, before: ${items.size}, object: ${this.toString()}")
        val p = PhotoData()
        items.add(p)
        Log.d("Arbeitsbericht.debug", "Adding photo, after: ${items.size}, object: ${this.toString()}")
        return p
    }

    fun removePhoto(p: PhotoData) {
        Log.d("Arbeitsbericht.debug", "Removing photo, before: ${items.size}, object: ${this.toString()}")
        items.remove(p)
        Log.d("Arbeitsbericht.debug", "Removing photo, after: ${items.size}, object: ${this.toString()}")
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