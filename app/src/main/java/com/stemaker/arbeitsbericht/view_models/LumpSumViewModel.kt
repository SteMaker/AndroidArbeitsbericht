package com.stemaker.arbeitsbericht.view_models

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stemaker.arbeitsbericht.data.report.*

class LumpSumContainerViewModel(
    private val lifecycleOwner: LifecycleOwner,
    private val lumpSumContainer: LumpSumContainerData,
    val definedLumpSums: LiveData<Set<String>>)
    : ViewModel(),
    Iterable<LumpSumViewModel>
{
    val visibility = lumpSumContainer.visibility
    private val itemViewModels = mutableListOf<LumpSumViewModel>()
    init {
        for(item in lumpSumContainer.items) {
            itemViewModels.add(LumpSumViewModel(lifecycleOwner, item))
        }
    }

    fun addLumpSum(): LumpSumViewModel {
        val ls = lumpSumContainer.addLumpSum()
        val viewModel = LumpSumViewModel(lifecycleOwner, ls)
        itemViewModels.add(viewModel)
        return viewModel
    }

    fun removeLumpSum(l: LumpSumViewModel) {
        itemViewModels.remove(l)
        lumpSumContainer.removeLumpSum(l.getData())
    }

    override fun iterator(): Iterator<LumpSumViewModel> {
        return object: Iterator<LumpSumViewModel> {
            var idx = 0
            override fun hasNext(): Boolean {
                return idx < itemViewModels.size
            }
            override fun next(): LumpSumViewModel {
                val r = itemViewModels[idx]
                idx++
                return r
            }
        }
    }
}

class LumpSumContainerViewModelFactory(
    private val lifecycleOwner: LifecycleOwner,
    private val lumpSumContainer: LumpSumContainerData,
    private val definedLumpSums: LiveData<Set<String>>
)
    : ViewModelProvider.Factory
{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LumpSumContainerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LumpSumContainerViewModel(lifecycleOwner, lumpSumContainer, definedLumpSums) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class LumpSumViewModel(private val lifecycleOwner: LifecycleOwner, private val lumpSum: LumpSumData): ViewModel() {
    val item = lumpSum.item
    val amount = lumpSum.amount
    val comment = lumpSum.comment
    // Use this only within the ContainerViewModel above
    fun getData(): LumpSumData = lumpSum
}
