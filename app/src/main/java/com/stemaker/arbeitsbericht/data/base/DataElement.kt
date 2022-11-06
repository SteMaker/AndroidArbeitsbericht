package com.stemaker.arbeitsbericht.data.base

import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport

interface DataElement {
    val elementName: String
    val propCS: PropertyChangeSupport

    fun firstObserver() {}
    fun lastObserverRemoved() {}

    // Members regarding the class being an event source
    fun addObserver(listener: PropertyChangeListener) {
        if(propCS.propertyChangeListeners.isEmpty()) firstObserver()
        propCS.addPropertyChangeListener(listener)
    }
    fun removeObserver(listener: PropertyChangeListener) {
        propCS.removePropertyChangeListener(listener)
        if(propCS.propertyChangeListeners.isEmpty()) lastObserverRemoved()
    }
}