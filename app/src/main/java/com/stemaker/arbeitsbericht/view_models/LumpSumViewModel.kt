package com.stemaker.arbeitsbericht.view_models

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stemaker.arbeitsbericht.data.report.*

class LumpSumContainerViewModel(
    private val lifecycleOwner: LifecycleOwner,
    private val lumpSumContainer: LumpSumContainerData)
    : ViewModel(),
    Iterable<LumpSumViewModel>
{
    val visibility = lumpSumContainer.visibility
    val lumpSumList = lumpSumContainer.list
    private val itemViewModels = mutableListOf<LumpSumViewModel>()
    init {
        for(item in lumpSumContainer.items) {
            itemViewModels.add(LumpSumViewModel(lifecycleOwner, item))
        }
    }

    fun addLumpSum(): LumpSumViewModel {
        val ls = lumpSumContainer.addLumpSum()
        return LumpSumViewModel(lifecycleOwner, ls)
    }

    fun removeLumpSum(l: LumpSumViewModel) {
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

class LumpSumContainerViewModelFactory(private val lifecycleOwner: LifecycleOwner, private val lumpSumContainer: LumpSumContainerData) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LumpSumContainerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LumpSumContainerViewModel(lifecycleOwner, lumpSumContainer) as T
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