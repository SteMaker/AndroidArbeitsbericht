package com.stemaker.arbeitsbericht

import android.content.Context
import androidx.lifecycle.MutableLiveData
import kotlinx.serialization.*

@Serializable
class Configuration() {
    var employeeName: String = ""
    var currentId: Int = 1
    var recvMail: String = ""
    var lumpSums = listOf<String>()
    var activeReportId: Int = -1
    var workItemDictionary = setOf<String>()
    var materialDictionary = setOf<String>()
}