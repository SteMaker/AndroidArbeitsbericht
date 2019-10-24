package com.stemaker.arbeitsbericht.helpers

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.stemaker.arbeitsbericht.R
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun showConfirmationDialog(title: String, context: Context): Int {
        var continuation: Continuation<Int>? = null
        Log.d("Arbeitsbericht.ConfirmationFragment.showConfirmationDialog", "in coroutine context")
        val alert = AlertDialog.Builder(context)
            .setIcon(R.drawable.ic_delete_black_24dp)
            .setTitle(title)
            .setPositiveButton(R.string.ok) { _, button -> continuation!!.resume(button) }
            .setNegativeButton(R.string.cancel) { _, button -> continuation!!.resume(button) }
            .setOnCancelListener() { _ -> continuation!!.resume(AlertDialog.BUTTON_NEUTRAL) }
            .create()
        alert.show()
        return suspendCoroutine<Int> {
            Log.d("Arbeitsbericht.ConfirmationFragment.showConfirmationDialog", "Coroutine: suspended")
            continuation = it
        }
}