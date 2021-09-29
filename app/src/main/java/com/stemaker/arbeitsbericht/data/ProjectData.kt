package com.stemaker.arbeitsbericht.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProjectData() : ViewModel() {
    val name = MutableLiveData<String>().apply { value = "" }
    val extra1 = MutableLiveData<String>().apply { value = "" }
    val visibility = MutableLiveData<Boolean>().apply { value = true }
    var clientId: Int = Int.MAX_VALUE

    fun copyFromSerialized(p: ProjectDataSerialized) {
        name.value = p.name
        extra1.value = p.extra1
        visibility.value = p.visibility
    }
    fun copyFromDb(p: ProjectDb) {
        name.value = p.projectName
        extra1.value = p.extra1
        visibility.value = p.projectVisibility
        clientId = p.clientId
    }
}