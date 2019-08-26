package com.stemaker.arbeitsbericht

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TextView
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment

class TimePickerFragment(_tv: TextView) : DialogFragment(), TimePickerDialog.OnTimeSetListener {
    val tv = _tv

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use the current time as the default values for the picker
        val hour: Int
        val minute : Int

        if(tv.text == "") {
            hour = 0
            minute = 0
        } else {
            val timeString = tv.text
            hour = timeString.substring(0,2).toInt()
            minute = timeString.substring(3,5).toInt()
        }
        // Create a new instance of TimePickerDialog and return it
        return TimePickerDialog(activity, this, hour, minute, true)
    }

    override fun onTimeSet(view: TimePicker, hour: Int, minute: Int) {
        val timeString = hour.toString().padStart(2,'0') + ":" +
                         minute.toString().padStart(2,'0')
        tv.setText(timeString)
    }
}
