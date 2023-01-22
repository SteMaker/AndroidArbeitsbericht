package com.stemaker.arbeitsbericht.data.report

import com.stemaker.arbeitsbericht.data.base.*

const val PHOTO_CONTAINER_VISIBILITY = "pcVis"
const val PHOTO_CONTAINER = "pc"
class PhotoContainerData: DataContainer<PhotoData>(PHOTO_CONTAINER) {
    val visibility = DataElement<Boolean>(PHOTO_CONTAINER_VISIBILITY) { false }

    /*
    fun copyFromSerialized(p: PhotoContainerDataSerialized) {
        visibility.value = p.visibility
        clear()
        for(i in 0 until p.items.size) {
            val item = PhotoData()
            item.copyFromSerialized(p.items[i])
            add(item)
        }
    }
     */

    fun copyFromDb(w: PhotoContainerDb) {
        visibility.value = w.pVisibility
        clear()
        for(element in w.pItems) {
            val item = PhotoData()
            item.copyFromDb(element)
            add(item)
        }
    }

    fun copy() {
        // not copying photos
    }

    fun addPhoto(): PhotoData {
        val p = PhotoData()
        add(p)
        return p
    }

    fun removePhoto(p: PhotoData) {
        remove(p)
    }
}

const val PHOTO_DATA = "pData"
const val PHOTO_FILE = "pFile"
const val PHOTO_DESCRIPTION = "pDesc"
const val PHOTO_HEIGHT = "pHeight"
const val PHOTO_WIDTH = "pWidth"
class PhotoData: DataObject(PHOTO_DATA) {

    val file = DataElement<String>(PHOTO_FILE) { "" }
    val description = DataElement<String>(PHOTO_DESCRIPTION) { "" }
    val imageHeight = DataElement<Int>(PHOTO_HEIGHT) { 0 }
    val imageWidth = DataElement<Int>(PHOTO_WIDTH) { 0 }

    override val elements = listOf<DataBasicIf>(
        file, description, imageHeight, imageWidth
    )

    /*
    fun copyFromSerialized(p: PhotoDataSerialized) {
        file.value = p.file
        description.value = p.description
        imageHeight.value = p.imageHeight
        imageWidth.value = p.imageWidth
        // Normally this was already filled in at the time of taking the photo. But in case this was taken with an older app version, it might not
        if(imageWidth.value!! <= 0 || imageHeight.value!! <= 0) {
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            val bitmap = BitmapFactory.decodeFile(p.file, options)
            if(bitmap != null) {
                imageWidth.value = bitmap.width
                imageHeight.value = bitmap.height
            } else { // image not found
                imageWidth.value = 1
                imageHeight.value = 1
            }
        }
    }
    */
    fun copyFromDb(p: PhotoContainerDb.PhotoDb) {
        file.value = p.pFile
        description.value = p.pDescription
        imageHeight.value = p.pImageHeight
        imageWidth.value = p.pImageWidth
    }
}