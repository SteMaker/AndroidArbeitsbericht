package com.stemaker.arbeitsbericht.data.report

import com.stemaker.arbeitsbericht.data.base.DataBasicIf
import com.stemaker.arbeitsbericht.data.base.DataObject
import com.stemaker.arbeitsbericht.data.base.DataElement

const val BILL_NAME = "billName"
const val BILL_STREET = "billStreet"
const val BILL_ZIP = "billZip"
const val BILL_CITY = "billCity"
const val BILL_VISIBILITY = "billVis"
const val BILL = "bill"
class BillData: DataObject(BILL) {

    val name = DataElement<String>(BILL_NAME) { "" }
    val street = DataElement<String>(BILL_STREET) { "" }
    val zip = DataElement<String>(BILL_ZIP) { "" }
    val city = DataElement<String>(BILL_CITY) { "" }
    val visibility = DataElement<Boolean>(BILL_VISIBILITY) { false }

    override val elements = listOf<DataBasicIf>(
        name, street, zip, city, visibility
    )

    /*
    fun copyFromSerialized(b: BillDataSerialized) {
        name.value = b.name
        street.value = b.street
        zip.value = b.zip
        city.value = b.city
        visibility.value = b.visibility
    }
     */

    fun copyFromDb(b: BillDb) {
        name.value = b.billName
        street.value = b.street
        zip.value = b.zip
        city.value = b.city
        visibility.value = b.billVisibility
    }

    fun copy(b: BillData) {
        name.copy(b.name)
        street.copy(b.street)
        zip.copy(b.zip)
        city.copy(b.city)
        visibility.copy(b.visibility)
    }
}