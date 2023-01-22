package com.stemaker.arbeitsbericht.helpers

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import java.util.*

// TODO: Put the button and TextView into a separate layout xml and inflate it here directly using data binding instead of letting the higher level
//       XML handle it

class DatePickerFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {
    lateinit var date: MutableLiveData<Calendar>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val year = date.value?.get(Calendar.YEAR)?:1970
        val month = date.value?.get(Calendar.MONTH)?:0
        val day = date.value?.get(Calendar.DAY_OF_MONTH)?:1

        // Create a new instance of DatePickerDialog and return it
        return DatePickerDialog(requireActivity(), this, year, month, day)
    }

    override fun onPause() {
        dismiss()
        super.onPause()
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        date.value?.set(Calendar.YEAR, year)
        date.value?.set(Calendar.MONTH, month)
        date.value?.set(Calendar.DAY_OF_MONTH, day)
        date.let {it.value = it.value}
    }
}
