package com.stemaker.arbeitsbericht.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProjectData() : ViewModel() {
    val name = MutableLiveData<String>().apply { value = "" }
    val extra1 = MutableLiveData<String>().apply { value = "" }

    fun copyFromSerialized(p: ProjectDataSerialized) {
        name.value = p.name
        extra1.value = p.extra1
    }
}