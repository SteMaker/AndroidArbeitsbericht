package com.stemaker.arbeitsbericht.view_models

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stemaker.arbeitsbericht.data.report.*

class BillViewModel(private val lifecycleOwner: LifecycleOwner, bill: BillData): ViewModel() {
    val name = bill.name
    val street = bill.street
    val zip = bill.zip
    val city = bill.city
    val visibility = bill.visibility
}

class BillViewModelFactory(private val lifecycleOwner: LifecycleOwner, private val bill: BillData) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BillViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BillViewModel(lifecycleOwner, bill) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
