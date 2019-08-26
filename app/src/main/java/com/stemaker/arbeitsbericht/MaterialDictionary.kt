package com.stemaker.arbeitsbericht

import kotlinx.serialization.Serializable

@Serializable
class MaterialDictionary() {
    var items = mutableSetOf<String>()
}