package com.stemaker.arbeitsbericht.data

import java.util.*

fun calendarToDateString(c: Calendar?): String {
    return c?.let {
        it.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0') + "." +
                (it.get(Calendar.MONTH) + 1).toString().padStart(2, '0') + "." +
                it.get(Calendar.YEAR).toString().padStart(4, '0')
    } ?: ""
}

fun dateStringToCalendar(c: String): Calendar {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, c.substring(0,2).toInt())
    cal.set(Calendar.MONTH, c.substring(3,5).toInt()-1)
    cal.set(Calendar.YEAR, c.substring(6,10).toInt())
    return cal
}