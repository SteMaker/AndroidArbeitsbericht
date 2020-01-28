package com.stemaker.arbeitsbericht.helpers

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.stemaker.arbeitsbericht.R
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun showConfirmationDialog(title: String, context: Context, msg: String=""): Int {
    var continuation: Continuation<Int>? = null
    val alert = AlertDialog.Builder(context)
        .setIcon(R.drawable.ic_delete_black_24dp)
        .setTitle(title)
        .setMessage(msg)
        .setPositiveButton(R.string.ok) { _, button -> continuation!!.resume(button) }
        .setNegativeButton(R.string.cancel) { _, button -> continuation!!.resume(button) }
        .setOnCancelListener() { _ -> continuation!!.resume(AlertDialog.BUTTON_NEUTRAL) }
        .create()
    alert.show()
    return suspendCoroutine<Int> {
        continuation = it
    }
}

suspend fun showInfoDialog(title: String, context: Context, msg: String=""): Int {
        var continuation: Continuation<Int>? = null
        val alert = AlertDialog.Builder(context)
            .setIcon(R.drawable.ic_info_outline_black_24dp)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton(R.string.ok) { _, button -> continuation!!.resume(AlertDialog.BUTTON_NEUTRAL) }
            .setOnCancelListener() { _ -> continuation!!.resume(AlertDialog.BUTTON_NEUTRAL) }
            .create()
        alert.show()
        return suspendCoroutine<Int> {
            continuation = it
        }
}