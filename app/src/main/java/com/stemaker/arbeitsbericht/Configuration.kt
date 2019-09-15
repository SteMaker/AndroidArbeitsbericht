package com.stemaker.arbeitsbericht

import android.content.Context
import kotlinx.serialization.*

@Serializable
class Configuration() {
    var employeeName: String = ""
    var currentId: Int = 1
    var recvMail: String = ""
    var lumpSums = mutableListOf<String>()
}