package com.stemaker.arbeitsbericht.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PhotoContainerData(): ViewModel() {
    val items = mutableListOf<PhotoData>()
    val visibility = MutableLiveData<Boolean>().apply { value = false }

    fun copyFromSerialized(p: PhotoContainerDataSerialized) {
        visibility.value = p.visibility
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
    var imageHeight = 0
    var imageWidth = 0

    fun copyFromSerialized(p: PhotoDataSerialized) {
        file.value = p.file
        description.value = p.description
        imageHeight = p.imageHeight
        imageWidth = p.imageWidth
        // Normally this was already filled in at the time of taking the photo. But in case this was taken with an older app version, it might not
        if(imageWidth <= 0 || imageHeight <= 0) {
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            val bitmap = BitmapFactory.decodeFile(p.file, options)
            imageWidth = bitmap.width
            imageHeight = bitmap.height
        }
    }
}