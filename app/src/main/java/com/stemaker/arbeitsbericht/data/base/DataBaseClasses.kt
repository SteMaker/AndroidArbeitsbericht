package com.stemaker.arbeitsbericht.data.base

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations

/* Event to be emitted whenever a DataElement, DataObject or DataContainer changes */
class DataModificationEvent(val type: Type, var elem: DataBasicIf) {
    enum class Type {
        CONTAINER_ADD, CONTAINER_REMOVE, CONTAINER_CLEAR, ELEMENT_CHANGE
    }
}
/* Interface to be used for any DataElement, DataObject or DataContainer */
interface DataBasicIf {
    var elementName: String
    val dataModificationEvent: LiveData<DataModificationEvent>
}

/* This is the base class to be used for primitive report elements */
class DataElement<T>(n: String, initFct: (() -> T) ):
    MutableLiveData<T>(),
    DataBasicIf
{
    init {
        setValue(initFct())
    }
    override var elementName: String = n
    override val dataModificationEvent = Transformations.map(this) {
        if(this.elementName == "report.bill.billVis") {
            Log.d("abc", "here we are")
        }
        DataModificationEvent(DataModificationEvent.Type.ELEMENT_CHANGE, this)
    }
    fun copy(origin: DataElement<T>) {
        value = origin.value
    }
}

/* This is the class to be used for report objects, i.e. such that contain a set of DataElement */
abstract class DataObject(n: String):
    DataBasicIf
{
    override var elementName: String = n
    abstract val elements: List<DataBasicIf>
    final override val dataModificationEvent: MediatorLiveData<DataModificationEvent> by lazy {
        val mld = MediatorLiveData<DataModificationEvent>()
        for(e in elements) {
            e.elementName = "${elementName}.${e.elementName}"
            mld.addSource(e.dataModificationEvent) { value ->
                mld.value = value
            }
        }
        mld
    }
}

/* Container to hold a list of DataElement or DataObject*/
open class DataContainer<T: DataBasicIf>(n: String):
    DataBasicIf,
    Iterable<T>
{
    override var elementName = n

    val items = mutableListOf<T>()

    // the clear, add, remove, ... events of the container
    val containerModificationEvent = MutableLiveData<DataModificationEvent>()
    // merge of above container events with the ones from the items
    final override val dataModificationEvent = MediatorLiveData<DataModificationEvent>()
    init {
        dataModificationEvent.addSource(containerModificationEvent) { value ->
            dataModificationEvent.value = value
        }
    }

    fun clear() {
        for(i in items)
            dataModificationEvent.removeSource(i.dataModificationEvent)
        items.clear()
        containerModificationEvent.value = DataModificationEvent(DataModificationEvent.Type.CONTAINER_CLEAR, this)
    }
    fun add(elem: T) {
        items.add(elem)
        elem.elementName = "${elementName}[${items.indexOf(elem)}].${elem.elementName}"
        dataModificationEvent.addSource(elem.dataModificationEvent) { value ->
            dataModificationEvent.value = value
        }
        containerModificationEvent.value = DataModificationEvent(DataModificationEvent.Type.CONTAINER_ADD, elem)
    }
    fun remove(elem: T) {
        items.remove(elem)
        dataModificationEvent.removeSource(elem.dataModificationEvent)
        containerModificationEvent.value = DataModificationEvent(DataModificationEvent.Type.CONTAINER_REMOVE, elem)
    }
    val size = items.size

    override fun iterator(): Iterator<T> {
        return object: Iterator<T> {
            var idx = 0
            override fun hasNext(): Boolean {
                return idx<items.size
            }
            override fun next(): T {
                val r = items[idx]
                idx++
                return r
            }
        }
    }
}
