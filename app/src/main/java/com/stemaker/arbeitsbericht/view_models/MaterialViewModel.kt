package com.stemaker.arbeitsbericht.view_models

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stemaker.arbeitsbericht.data.report.*

class MaterialContainerViewModel(
    private val lifecycleOwner: LifecycleOwner,
    private val materialContainer: MaterialContainerData,
    val definedMaterials: LiveData<Set<String>>)
    : ViewModel(),
    Iterable<MaterialViewModel>
{
    val visibility = materialContainer.visibility
    val units = materialContainer.units
    private val itemViewModels = mutableListOf<MaterialViewModel>()
    init {
        for(item in materialContainer.items) {
            itemViewModels.add(MaterialViewModel(lifecycleOwner, item))
        }
    }

    fun addMaterial(): MaterialViewModel {
        val m = materialContainer.addMaterial()
        return MaterialViewModel(lifecycleOwner, m)
    }
    fun removeMaterial(m: MaterialViewModel) {
        materialContainer.removeMaterial(m.getData())
    }

    override fun iterator(): Iterator<MaterialViewModel> {
        return object: Iterator<MaterialViewModel> {
            var idx = 0
            override fun hasNext(): Boolean {
                return idx < itemViewModels.size
            }
            override fun next(): MaterialViewModel {
                val r = itemViewModels[idx]
                idx++
                return r
            }
        }
    }
}

class MaterialContainerViewModelFactory(
    private val lifecycleOwner: LifecycleOwner,
    private val materialContainer: MaterialContainerData,
    private val definedMaterials: LiveData<Set<String>>)
    : ViewModelProvider.Factory
{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MaterialContainerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MaterialContainerViewModel(lifecycleOwner, materialContainer, definedMaterials) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
class MaterialViewModel(private val lifecycleOwner: LifecycleOwner, private val material: MaterialData): ViewModel() {
    val item = material.item
    val amount = material.amount
    val unit = material.unit
    // Use this only within the ContainerViewModel above
    fun getData(): MaterialData = material
}