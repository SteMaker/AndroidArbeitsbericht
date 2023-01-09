package com.stemaker.arbeitsbericht.data.report

import android.util.Log
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import com.stemaker.arbeitsbericht.BR
import com.stemaker.arbeitsbericht.data.base.DataBasicIf
import com.stemaker.arbeitsbericht.data.base.DataObject
import com.stemaker.arbeitsbericht.data.base.DataElement
import com.stemaker.arbeitsbericht.data.base.DataModificationEvent

const val PROJECT_NAME = "projName"
const val PROJECT_EXTRA1 = "projExtra1"
const val PROJECT_VISIBILITY = "projVis"
const val PROJECT = "proj"

class ProjectData: DataObject(PROJECT) {

    val name = DataElement<String>(PROJECT_NAME) { "" }
    val extra1 = DataElement<String>(PROJECT_EXTRA1) { "" }
    val visibility = DataElement<Boolean>(PROJECT_VISIBILITY) { true }
    var clientId: Int = Int.MAX_VALUE

    override val elements = listOf<DataBasicIf>(
        name, extra1, visibility
    )

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

    fun copy(p: ProjectData) {
        name.copy(p.name)
        extra1.copy(p.extra1)
        visibility.copy(p.visibility)
        clientId = p.clientId
    }
}