package com.stemaker.arbeitsbericht.data.base

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport

/* Base class for report data containers. I.e. such that maintain a list of DataObjects */
open class DataContainer<T: DataObject>(n: String):
    DataElement,
    PropertyChangeListener {
    override val propCS : PropertyChangeSupport by lazy {
        PropertyChangeSupport(this)
    }

    override val elementName = n
    val items = mutableListOf<T>()

    //////////////////////////////////////////////////////
    // Members regarding the class being an event receiver
    //////////////////////////////////////////////////////
    override fun propertyChange(ev: PropertyChangeEvent) {
        propCS.firePropertyChange(ev)
    }

    ////////////////////////////
    // Collection handling stuff
    ////////////////////////////
    fun add(e: T) {
        items.add(e)
        propCS.firePropertyChange(ListChangeEvent<DataContainer<T>, T>(this, ListChangeEvent.ListEvent.ADD, e))
        e.addObserver(this)
    }
    fun remove(e: T) {
        e.removeObserver(this)
        items.remove(e)
        propCS.firePropertyChange(ListChangeEvent<DataContainer<T>, T>(this, ListChangeEvent.ListEvent.REMOVE, e))
        items.iterator()
    }
    val size: Int
        get() = items.size
    fun clear() = items.clear()
    operator fun iterator() = items.iterator()
    operator fun get(i: Int) = items[i]
}