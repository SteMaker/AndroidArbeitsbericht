package com.stemaker.arbeitsbericht.view_models

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stemaker.arbeitsbericht.data.report.*

class PhotoContainerViewModel(
    private val lifecycleOwner: LifecycleOwner,
    private val photoContainer: PhotoContainerData)
    : ViewModel(),
    Iterable<PhotoViewModel>
{
    val visibility = photoContainer.visibility
    private val itemViewModels = mutableListOf<PhotoViewModel>()
    init {
        for(item in photoContainer.items) {
            itemViewModels.add(PhotoViewModel(lifecycleOwner, item))
        }
    }

    fun addPhoto(): PhotoViewModel {
        val p = photoContainer.addPhoto()
        val viewModel = PhotoViewModel(lifecycleOwner, p)
        itemViewModels.add(viewModel)
        return viewModel
    }
    fun removePhoto(p: PhotoViewModel) {
        itemViewModels.remove(p)
        photoContainer.removePhoto(p.getData())
    }

    override fun iterator(): Iterator<PhotoViewModel> {
        return object: Iterator<PhotoViewModel> {
            var idx = 0
            override fun hasNext(): Boolean {
                return idx < itemViewModels.size
            }
            override fun next(): PhotoViewModel {
                val r = itemViewModels[idx]
                idx++
                return r
            }
        }
    }
}

class PhotoContainerViewModelFactory(
    private val lifecycleOwner: LifecycleOwner,
    private val photoContainer: PhotoContainerData)
    : ViewModelProvider.Factory
{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhotoContainerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PhotoContainerViewModel(lifecycleOwner, photoContainer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class PhotoViewModel(
    private val lifecycleOwner: LifecycleOwner,
    private val photo: PhotoData)
    : ViewModel()
{
    val file = photo.file
    val description = photo.description
    val imageHeight = photo.imageHeight
    val imageWidth = photo.imageWidth

    // Use this only within the ContainerViewModel above
    fun getData(): PhotoData = photo
}