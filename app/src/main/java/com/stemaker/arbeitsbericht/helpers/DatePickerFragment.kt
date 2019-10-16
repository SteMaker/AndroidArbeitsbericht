package com.stemaker.arbeitsbericht.helpers

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import java.util.*

// TODO: Put the button and TextView into a separate layout xml and inflate it here directly using data binding instead of letting the higher level
//       XML handle it

class DatePickerFragment(_dateString: MutableLiveData<String>) : DialogFragment(), DatePickerDialog.OnDateSetListener {
    val dateString = _dateString
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val year: Int
        val month: Int
        val day: Int
        if(dateString.value == "") {
            // Use the current date as the default date in the picker
            val c = Calendar.getInstance()
            year = c.get(Calendar.YEAR)
            month = c.get(Calendar.MONTH)
            day = c.get(Calendar.DAY_OF_MONTH)
        } else {
            val dateVal = dateString.value!!
            Log.d("Arbeitsbericht.DatePickerFragment.onCreateDialog", "Default date: ${dateVal}")
            day = dateVal.substring(0,2).toInt()
            month = dateVal.substring(3,5).toInt()
            year = dateVal.substring(6,10).toInt()
        }

        // Create a new instance of DatePickerDialog and return it
        return DatePickerDialog(activity, this, year, month-1, day)
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        dateString.value = day.toString().padStart(2,'0') + "." +
                (month+1).toString().padStart(2,'0') + "." +
                year.toString().padStart(4,'0')
        Log.d("DatePickerFragment.onDateSet", "New Date: $dateString")
    }
}
