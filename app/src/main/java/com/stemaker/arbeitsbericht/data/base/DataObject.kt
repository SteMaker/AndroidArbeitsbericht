package com.stemaker.arbeitsbericht.data.base

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport

/* Base class for DataObjects, i.e. such that hold a set of ReportDataElement or ReportDataElementList  */
abstract class DataObject(n: String):
    DataElement,
    PropertyChangeListener {
    override val propCS : PropertyChangeSupport by lazy {
        PropertyChangeSupport(this)
    }
    override val elementName = n

    private val elements = mutableListOf<DataElement>()

    //////////////////////////////////////////////////////
    // Members regarding the class being an event receiver
    //////////////////////////////////////////////////////
    override fun propertyChange(ev: PropertyChangeEvent) {
        propCS.firePropertyChange(ev)
    }

    fun registerElement(e: DataElement) {
        elements.add(e)
        e.addObserver(this)
    }
}