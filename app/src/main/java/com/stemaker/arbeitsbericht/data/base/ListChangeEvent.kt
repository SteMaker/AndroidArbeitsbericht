package com.stemaker.arbeitsbericht.data.base

import java.beans.PropertyChangeEvent

/* T1 is the type of the list, T2 is the type of the list element
 * Source is the list object, e is the added / removed element
 */
class ListChangeEvent<T1, T2: DataElement>(source: T1, val ev: ListEvent, val e: T2):
    PropertyChangeEvent(source, e.elementName, null, null) {
    enum class ListEvent {
        ADD, REMOVE
    }
}