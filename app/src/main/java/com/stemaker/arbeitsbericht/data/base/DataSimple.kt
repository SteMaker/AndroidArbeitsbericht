package com.stemaker.arbeitsbericht.data.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.beans.PropertyChangeSupport

/* This is the class to be used for single report elements that require observability. Do not use for lists */
open class DataSimple<T>(initValue: T, n: String):
    DataElement,
    MutableLiveData<T>() {
    override val elementName = n
    override val propCS : PropertyChangeSupport by lazy {
        PropertyChangeSupport(this)
    }
    private val observer = Observer<T> { propCS.firePropertyChange(ElementChangeEvent<DataSimple<T>,DataSimple<T>>(this@DataSimple, this@DataSimple)) }
    override fun firstObserver() {
        observeForever(observer)
    }
    override fun lastObserverRemoved() {
        removeObserver(observer)
    }
    init {
        value = initValue
    }
}
