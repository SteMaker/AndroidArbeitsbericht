package com.stemaker.arbeitsbericht.view_models

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stemaker.arbeitsbericht.data.report.SignatureData

class SignatureViewModel(
    private val lifecycleOwner: LifecycleOwner,
    private val signature: SignatureData)
    : ViewModel()
{
    val employeeSignatureSvg = signature.employeeSignatureSvg
    val clientSignatureSvg = signature.clientSignatureSvg

    fun isEmployeeSignatureDefined(): Boolean = signature.employeeSignatureSvg.value?.let { it != "" } ?: false
    fun isClientSignatureDefined(): Boolean = signature.clientSignatureSvg.value?.let { it != "" } ?: false
    fun getEmployeeSignatureSvg(): String = signature.employeeSignatureSvg.value ?: ""
    fun getClientSignatureSvg(): String = signature.clientSignatureSvg.value ?: ""
    fun clearEmployeeSignature() { signature.employeeSignatureSvg.value = "" }
    fun clearClientSignature() { signature.clientSignatureSvg.value = "" }
    fun setEmployeeSignatureSvg(svg: String) { signature.employeeSignatureSvg.value = svg }
    fun setClientSignatureSvg(svg: String) { signature.clientSignatureSvg.value = svg }
}

class SignatureViewModelFactory(private val lifecycleOwner: LifecycleOwner, private val signature: SignatureData) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignatureViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SignatureViewModel(lifecycleOwner, signature) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
