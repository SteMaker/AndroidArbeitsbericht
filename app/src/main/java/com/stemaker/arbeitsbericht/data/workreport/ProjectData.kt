package com.stemaker.arbeitsbericht.data.workreport

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProjectData() : ViewModel() {
    val name = MutableLiveData<String>().apply { value = "" }
    val extra1 = MutableLiveData<String>().apply { value = "" }
    val visibility = MutableLiveData<Boolean>().apply { value = true }

    fun copyFromSerialized(p: ProjectDataSerialized) {
        name.value = p.name
        extra1.value = p.extra1
        visibility.value = p.visibility
    }
}