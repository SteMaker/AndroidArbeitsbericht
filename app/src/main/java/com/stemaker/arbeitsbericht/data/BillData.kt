package com.stemaker.arbeitsbericht.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BillData() : ViewModel() {
    val name = MutableLiveData<String>().apply { value = ""}
    val street = MutableLiveData<String>().apply { value = ""}
    val zip = MutableLiveData<String>().apply { value = ""}
    val city = MutableLiveData<String>().apply { value = ""}
    val visibility = MutableLiveData<Boolean>().apply { value = false }

    fun copyFromSerialized(b: BillDataSerialized) {
        name.value = b.name
        street.value = b.street
        zip.value = b.zip
        city.value = b.city
        visibility.value = b.visibility
    }

    fun copyFromDb(b: BillDb) {
        name.value = b.billName
        street.value = b.street
        zip.value = b.zip
        city.value = b.city
        visibility.value = b.billVisibility
    }
}