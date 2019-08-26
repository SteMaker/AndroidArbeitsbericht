package com.stemaker.arbeitsbericht

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

fun newConfirmationDialog(title: String): ConfirmationDialogFragment {
    val frag = ConfirmationDialogFragment()
    val args = Bundle()
    args.putString("title", title)
    frag.setArguments(args)
    return frag
}

class ConfirmationDialogFragment : DialogFragment() {
    // Use this instance of the interface to deliver action events
    internal lateinit var listener: ConfirmationDialogListener

    interface ConfirmationDialogListener {
        fun onDialogPositiveClick(dialog: DialogFragment)
        fun onDialogNegativeClick(dialog: DialogFragment)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            listener = context as ConfirmationDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException((context.toString() +
                    " must implement ConfirmationDialogListener"))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title: String? = arguments?.getString("title")

        return AlertDialog.Builder(activity!!)
            .setIcon(R.drawable.ic_delete_black_24dp)
            .setTitle(title)
            .setPositiveButton(R.string.ok, DialogInterface.OnClickListener {_, _ -> listener.onDialogPositiveClick(this)})
            .setNegativeButton(R.string.cancel, DialogInterface.OnClickListener {_, _ -> listener.onDialogNegativeClick(this)})
            .create()
    }
}
