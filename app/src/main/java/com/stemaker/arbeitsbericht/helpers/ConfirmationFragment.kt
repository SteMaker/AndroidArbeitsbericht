package com.stemaker.arbeitsbericht.helpers

import android.content.Context
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

enum class OutputType(val value: Int) {
    PDF(0),
    ODT(1),
    XLSX(2),
    UNKNOWN(0xFF);

    companion object {
        private val map = OutputType.values().associateBy(OutputType::value)
        fun fromInt(type: Int) = map[type]
    }
}
suspend fun showOutputSelectDialog(context: Context): OutputType {
        var continuation: Continuation<OutputType>? = null
        val alert = AlertDialog.Builder(context)
            .setTitle(R.string.output_select_dialog_title)
            .setItems(R.array.output_options_array) { _, which -> when(which) {
                0, 1, 2 -> continuation!!.resume(OutputType.fromInt(which)!!)
                else -> continuation!!.resume(OutputType.UNKNOWN) }
            }
            .create()
        alert.show()
        return suspendCoroutine<OutputType> {
            continuation = it
        }
}