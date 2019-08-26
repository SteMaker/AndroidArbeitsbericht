package com.stemaker.arbeitsbericht

import android.app.DatePickerDialog
import android.app.Dialog

import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import android.widget.TextView

import androidx.fragment.app.DialogFragment
import java.util.*

class DatePickerFragment(_tv: TextView) : DialogFragment(), DatePickerDialog.OnDateSetListener {
    val tv = _tv
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val year: Int
        val month: Int
        val day: Int
        if(tv.text == "") {
            // Use the current date as the default date in the picker
            val c = Calendar.getInstance()
            year = c.get(Calendar.YEAR)
            month = c.get(Calendar.MONTH)
            day = c.get(Calendar.DAY_OF_MONTH)
        } else {
            val dateString = tv.text
            day = dateString.substring(0,2).toInt()
            month = dateString.substring(3,5).toInt()
            year = dateString.substring(6,10).toInt()
        }

        // Create a new instance of DatePickerDialog and return it
        return DatePickerDialog(activity, this, year, month, day)
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        val dateString = day.toString().padStart(2,'0') + "." +
                (month+1).toString().padStart(2,'0') + "." +
                year.toString().padStart(4,'0')
        Log.d("DatePickerFragment.onDateSet", "New Date: $dateString")
        tv.setText(dateString)
    }
}
