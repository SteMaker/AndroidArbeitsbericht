package com.stemaker.arbeitsbericht

import kotlinx.serialization.Serializable

@Serializable
class WorkItemDictionary() {
    var items = mutableSetOf<String>()
}