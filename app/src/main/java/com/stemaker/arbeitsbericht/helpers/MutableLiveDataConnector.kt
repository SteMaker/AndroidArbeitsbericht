package com.stemaker.arbeitsbericht.helpers

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

class LiveDataConnector<T>(lifecycleOwner: LifecycleOwner, private val source: LiveData<T>): LiveData<T>() {

    override fun getValue(): T? {
        //Log.d("abc", "get")
        return super.getValue()
    }

    init {
        source.observe(lifecycleOwner, Observer<T> {
            //Log.d("abc", "observe")
            if(source.value != value)
                super.setValue(source.value)
        })
    }
}

class MutableLiveDataConnector<T>(lifecycleOwner: LifecycleOwner, private val source: MutableLiveData<T>): MutableLiveData<T>() {
    override fun setValue(newValue: T) {
        //Log.d("abc", "set")
        if(newValue != value) {
            super.setValue(newValue)
            newValue?.let {source.value = it}
        }
    }

    override fun getValue(): T? {
        //Log.d("abc", "get")
        return super.getValue()
    }

    init {
        source.observe(lifecycleOwner, Observer<T> {
            //Log.d("abc", "observe")
            if(source.value != value)
                super.setValue(source.value)
        })
    }
    fun getSource(): MutableLiveData<T> = source
}