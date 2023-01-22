package com.stemaker.arbeitsbericht.data.report

import com.stemaker.arbeitsbericht.data.base.*

const val LUMP_SUM_CONTAINER_VISIBILITY = "lscVis"
const val LUMP_SUM_CONTAINER = "lsc"
class LumpSumContainerData: DataContainer<LumpSumData>(LUMP_SUM_CONTAINER) {
    val visibility = DataElement<Boolean>(LUMP_SUM_CONTAINER_VISIBILITY) { false }

    /*
    fun copyFromSerialized(l: LumpSumContainerDataSerialized) {
        visibility.value = l.visibility
        clear()
        for(i in 0 until l.items.size) {
            val item = LumpSumData()
            item.copyFromSerialized(l.items[i])
            add(item)
        }
    }
     */

    fun copyFromDb(w: LumpSumContainerDb) {
        visibility.value = w.lVisibility
        clear()
        for(element in w.lItems) {
            val item = LumpSumData()
            item.copyFromDb(element)
            add(item)
        }
    }

    fun copy(w: LumpSumContainerData) {
        visibility.copy(w.visibility)
        clear()
        for(element in w.items) {
            val item = LumpSumData()
            item.copy(element)
            add(item)
        }
    }

    fun addLumpSum(): LumpSumData {
        val l = LumpSumData()
        add(l)
        return l
    }

    fun removeLumpSum(l: LumpSumData) {
        remove(l)
    }
}

const val LUMP_SUM_DATA = "lsData"
const val LUMP_SUM_ITEM = "lsItem"
const val LUMP_SUM_AMOUNT = "lsAmount"
const val LUMP_SUM_COMMENT = "lsComment"
class LumpSumData: DataObject(LUMP_SUM_DATA) {
    val item = DataElement<String>(LUMP_SUM_ITEM) { "" }
    val amount = DataElement<Int>(LUMP_SUM_AMOUNT) { 0 }
    val comment = DataElement<String>(LUMP_SUM_COMMENT) { "" }

    override val elements = listOf<DataBasicIf>(
        item, amount, comment
    )

    /*
    fun copyFromSerialized(l: LumpSumDataSerialized) {
        item.value = l.item
        amount.value = l.amount
        comment.value = l.comment
    }
     */

    fun copyFromDb(l: LumpSumContainerDb.LumpSumDb) {
        item.value = l.lItem
        amount.value = l.lAmount
        comment.value = l.lComment
    }

    fun copy(l: LumpSumData) {
        item.copy(l.item)
        amount.copy(l.amount)
        comment.copy(l.comment)
    }
}
