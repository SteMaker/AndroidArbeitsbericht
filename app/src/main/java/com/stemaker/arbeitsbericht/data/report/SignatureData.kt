package com.stemaker.arbeitsbericht.data.report

import com.stemaker.arbeitsbericht.data.base.DataBasicIf
import com.stemaker.arbeitsbericht.data.base.DataObject
import com.stemaker.arbeitsbericht.data.base.DataElement
import java.io.File


const val SIGNATURE = "sign"
const val SIGNATURE_EMPLOYEE_SVG = "signESvg"
const val SIGNATURE_CLIENT_SVG = "signCSvg"
class SignatureData: DataObject(SIGNATURE) {
    val employeeSignatureSvg = DataElement<String>(SIGNATURE_EMPLOYEE_SVG) { "" }
    val clientSignatureSvg = DataElement<String>(SIGNATURE_CLIENT_SVG) { "" }
    // Below won't be saved into JSON, are only used temporarily
    var employeeSignaturePngFile: File? = null
    var clientSignaturePngFile: File? = null

    override val elements = listOf<DataBasicIf>(
        employeeSignatureSvg, clientSignatureSvg
    )

    /*
    fun copyFromSerialized(s: SignatureDataSerialized) {
        employeeSignatureSvg.value = s.employeeSignatureSvg
        clientSignatureSvg.value = s.clientSignatureSvg
    }
     */

    fun copyFromDb(s: SignatureDb) {
        employeeSignatureSvg.value = s.employeeSignatureSvg
        clientSignatureSvg.value = s.clientSignatureSvg
    }

    fun copy() {
        // Not copying signatures
    }
}