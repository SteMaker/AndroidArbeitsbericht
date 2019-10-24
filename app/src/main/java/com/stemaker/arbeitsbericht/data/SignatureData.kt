package com.stemaker.arbeitsbericht.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SignatureData: ViewModel() {
    val employeeSignatureSvg = MutableLiveData<String>().apply { value = "" }
    val clientSignatureSvg = MutableLiveData<String>().apply { value = "" }

    fun copyFromSerialized(s: SignatureDataSerialized) {
        employeeSignatureSvg.value = s.employeeSignatureSvg
        clientSignatureSvg.value = s.clientSignatureSvg
    }
}