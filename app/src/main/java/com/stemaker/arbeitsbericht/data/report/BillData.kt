package com.stemaker.arbeitsbericht.data.report

import com.stemaker.arbeitsbericht.data.base.DataObject
import com.stemaker.arbeitsbericht.data.base.DataSimple

const val BILL_NAME = "billName"
const val BILL_STREET = "billStreet"
const val BILL_ZIP = "billZip"
const val BILL_CITY = "billCity"
const val BILL_VISIBILITY = "billVis"
const val BILL = "bill"
class BillData(): DataObject(BILL) {

    val name = DataSimple<String>("", BILL_NAME)
    val street = DataSimple<String>("", BILL_STREET)
    val zip = DataSimple<String>("", BILL_ZIP)
    val city = DataSimple<String>("", BILL_CITY)
    val visibility = DataSimple<Boolean>(false, BILL_VISIBILITY)

    init {
        registerElement(name)
        registerElement(street)
        registerElement(zip)
        registerElement(city)
        registerElement(visibility)
    }

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