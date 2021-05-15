package com.stemaker.arbeitsbericht.helpers

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.stemaker.arbeitsbericht.R
import kotlin.coroutines.Continuation

class OutputSelectDialogFragment: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var continuation: Continuation<OutputType>? = null
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setMessage(R.string.output_select_dialog_title)
                .setTitle(R.string.output_select_dialog_title)
                .setItems(R.array.output_options_array,
                    DialogInterface.OnClickListener { dialog, which ->
                            // The 'which' argument contains the index position
                            // of the selected item
                        })
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}