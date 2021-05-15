package com.stemaker.arbeitsbericht.helpers

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import java.util.*

// TODO: Put the button and TextView into a separate layout xml and inflate it here directly using data binding instead of letting the higher level
//       XML handle it

class TimePickerFragment(_timeString: MutableLiveData<String>) : DialogFragment(), TimePickerDialog.OnTimeSetListener {
    val timeString = _timeString

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use the current time as the default values for the picker
        val hour: Int
        val minute : Int

        if(timeString.value == "") {
            val c = Calendar.getInstance()
            hour = c.get(Calendar.HOUR_OF_DAY)
            minute = c.get(Calendar.MINUTE)
        } else {
            val timeVal = timeString.value!!
            hour = timeVal.substring(0,2).toInt()
            minute = timeVal.substring(3,5).toInt()
        }
        // Create a new instance of TimePickerDialog and return it
        return TimePickerDialog(activity, this, hour, minute, true)
    }

    override fun onTimeSet(view: TimePicker, hour: Int, minute: Int) {
        timeString.value = hour.toString().padStart(2,'0') + ":" +
                         minute.toString().padStart(2,'0')
    }
}
