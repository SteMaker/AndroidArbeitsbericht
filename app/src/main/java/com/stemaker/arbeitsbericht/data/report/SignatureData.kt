package com.stemaker.arbeitsbericht.data.report

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

class SignatureData: ViewModel() {
    val employeeSignatureSvg = MutableLiveData<String>().apply { value = "" }
    val clientSignatureSvg = MutableLiveData<String>().apply { value = "" }
    // Below won't be saved into JSON, are only used temporarily
    var employeeSignaturePngFile: File? = null
    var clientSignaturePngFile: File? = null

    fun copyFromSerialized(s: SignatureDataSerialized) {
        employeeSignatureSvg.value = s.employeeSignatureSvg
        clientSignatureSvg.value = s.clientSignatureSvg
    }

    fun copyFromDb(s: SignatureDb) {
        employeeSignatureSvg.value = s.employeeSignatureSvg
        clientSignatureSvg.value = s.clientSignatureSvg
    }
}