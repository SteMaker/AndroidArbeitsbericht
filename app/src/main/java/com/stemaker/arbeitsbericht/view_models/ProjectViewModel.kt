package com.stemaker.arbeitsbericht.view_models

import androidx.lifecycle.*
import com.stemaker.arbeitsbericht.data.report.*


class ProjectViewModel(val lifecycleOwner: LifecycleOwner, private val project: ProjectData): ViewModel() {
    var name = project.name
    var extra1 = project.extra1
    var visibility = project.visibility
}

class ProjectViewModelFactory(private val lifecycleOwner: LifecycleOwner, private val project: ProjectData) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProjectViewModel(lifecycleOwner, project) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
