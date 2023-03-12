package com.stemaker.arbeitsbericht.view_models

import androidx.lifecycle.*
import com.stemaker.arbeitsbericht.data.preferences.ConfigElement
import com.stemaker.arbeitsbericht.data.report.*

/* Dictionary strategy: Keep a local copy that is initialized with the already defined
   dictionary. Fill the local dictionary whenever focus on a respective text element is lost.
   When saving the report add the still existing work items to the prefs dictionary.
 */
class MaterialContainerViewModel(
    private val lifecycleOwner: LifecycleOwner,
    private val materialContainer: MaterialContainerData,
    val definedMaterials: ConfigElement<Set<String>>
)
    : ViewModel(),
    Iterable<MaterialViewModel>
{
    val visibility = materialContainer.visibility
    val materialDictionary = MutableLiveData<MutableSet<String>>()
    val units = materialContainer.units
    private val itemViewModels = mutableListOf<MaterialViewModel>()
    init {
        for(item in materialContainer.items) {
            itemViewModels.add(MaterialViewModel(lifecycleOwner, item))
        }
        materialDictionary.value = definedMaterials.value.toMutableSet()
    }

    fun addMaterial(): MaterialViewModel {
        val m = materialContainer.addMaterial()
        val viewModel = MaterialViewModel(lifecycleOwner, m)
        itemViewModels.add(viewModel)
        return viewModel
    }
    fun removeMaterial(m: MaterialViewModel) {
        itemViewModels.remove(m)
        materialContainer.removeMaterial(m.getData())
    }

    fun addToDictionary(dictEntry: String) {
        materialDictionary.value?.add(dictEntry)
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
    private val definedMaterials: ConfigElement<Set<String>>)
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