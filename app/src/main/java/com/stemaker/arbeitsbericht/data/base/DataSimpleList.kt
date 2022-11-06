package com.stemaker.arbeitsbericht.data.base

import android.util.Log
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport

private const val TAG = "SimpleList"

/* Base class for simple lists of elements */
open class DataSimpleList<T: DataSimple<*>>(n: String):
    DataElement,
    PropertyChangeListener {
    override val elementName = n
    override val propCS : PropertyChangeSupport by lazy {
        PropertyChangeSupport(this)
    }

    // Members regarding the class being an event receiver
    override fun propertyChange(ev: PropertyChangeEvent) {
        if (ev is ElementChangeEvent<*,*>) {
            propCS.firePropertyChange(ev)
        } else {
            Log.e(TAG, "Received unexpected property change event")
        }
    }
    ////////////////////////////
    // Collection handling stuff
    ////////////////////////////
    val items = mutableListOf<T>()
    fun add(e: T) {
        items.add(e)
        propCS.firePropertyChange(ListChangeEvent<DataSimpleList<T>, DataSimple<*>>(this, ListChangeEvent.ListEvent.ADD, e))
        e.addObserver(this)
    }
    fun remove(e: T) {
        e.removeObserver(this)
        items.remove(e)
        propCS.firePropertyChange(ListChangeEvent<DataSimpleList<T>, DataSimple<*>>(this, ListChangeEvent.ListEvent.REMOVE, e))
    }
    val size: Int
        get() = items.size
    fun clear() = items.clear()
    operator fun iterator() = items.iterator()
    operator fun get(i: Int) = items[i]
}