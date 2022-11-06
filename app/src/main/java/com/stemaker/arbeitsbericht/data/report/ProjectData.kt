package com.stemaker.arbeitsbericht.data.report

import com.stemaker.arbeitsbericht.data.base.DataObject
import com.stemaker.arbeitsbericht.data.base.DataSimple

const val PROJECT_NAME = "projName"
const val PROJECT_EXTRA1 = "projExtra1"
const val PROJECT_VISIBILITY = "projVis"
const val PROJECT = "proj"
class ProjectData(): DataObject(PROJECT) {

    val name = DataSimple<String>("", PROJECT_NAME)
    val extra1 = DataSimple<String>("", PROJECT_EXTRA1)
    val visibility = DataSimple<Boolean>(true, PROJECT_VISIBILITY)
    var clientId: Int = Int.MAX_VALUE

    init {
        registerElement(name)
        registerElement(extra1)
        registerElement(visibility)
    }
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