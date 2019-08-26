package com.stemaker.arbeitsbericht

import android.content.Context

class Configuration(c: Context) {
    var employeeName: String = c.getString(R.string.unknown)
    var currentId: Int = 1
    var recvMail: String = ""
}